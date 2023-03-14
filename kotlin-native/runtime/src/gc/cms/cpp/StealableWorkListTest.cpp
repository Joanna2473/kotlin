/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StealableWorkList.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"
#include "TestSupport.hpp"

#include "std_support/Deque.hpp"
#include "std_support/Vector.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

class Node : private Pinned {
public:
    explicit Node(int value) : value_(value) {}

    int& operator*() { return value_; }
    const int& operator*() const { return value_; }

    void clearNext() noexcept { next_ = nullptr; }

    int value() const {
        return value_;
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<Node>;

    Node* next() const noexcept { return next_; }
    void setNext(Node* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_ = next;
    }
    bool trySetNext(Node* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (next_) return false;
        next_ = next;
        return true;
    }

    int value_;
    Node* next_ = nullptr;
};

using TestSubject = StealableWorkList<Node>;

std_support::vector<int> range(int first, int lastExclusive) {
    std_support::vector<int> values;
    for (int i = first; i < lastExclusive; ++i) {
        values.push_back(i);
    }
    return values;
}

template<typename Values>
[[nodiscard]] std_support::list<typename TestSubject::value_type> fill(TestSubject& list, Values&& values) {
    std_support::list<typename TestSubject::value_type> nodesHandle;
    for (int value: values) {
        auto& elem = nodesHandle.emplace_back(value);
        list.tryPushLocal(elem);
    }
    return nodesHandle;
}

void drainLocalInto(TestSubject& list, std_support::vector<int>& dest) {
    while (auto elem = list.tryPopLocal()) {
        dest.push_back(elem->value());
    }
}

} // namespace

TEST(StealableWorkListTest, Init) {
    TestSubject list;
    EXPECT_THAT(list.localEmpty(), true);
    EXPECT_THAT(list.localSize(), 0);
    EXPECT_THAT(list.sharedEmpty(), true);
}

TEST(StealableWorkListTest, TryPopLocalEmpty) {
    TestSubject list;
    auto res = list.tryPopLocal();
    EXPECT_THAT(res, nullptr);
}

TEST(StealableWorkListTest, TryPushLocalPopLocal) {
    TestSubject list;
    typename TestSubject::value_type value1(1);
    typename TestSubject::value_type value2(2);
    bool pushed1 = list.tryPushLocal(value1);
    bool pushed2 = list.tryPushLocal(value2);
    EXPECT_THAT(pushed1, true);
    EXPECT_THAT(pushed2, true);
    EXPECT_THAT(list.localEmpty(), false);
    EXPECT_THAT(list.localSize(), 2);
    EXPECT_THAT(list.sharedEmpty(), true);
    std_support::vector<int> popped;
    drainLocalInto(list, popped);
    EXPECT_THAT(list.localEmpty(), true);
    EXPECT_THAT(list.localSize(), 0);
    EXPECT_THAT(list.sharedEmpty(), true);
    EXPECT_THAT(popped, testing::UnorderedElementsAre(1, 2));
}

TEST(StealableWorkListTest, TryPushLocalTwice) {
    TestSubject list;
    typename TestSubject::value_type value(1);
    bool pushed1 = list.tryPushLocal(value);
    EXPECT_THAT(pushed1, true);
    bool pushed2 = list.tryPushLocal(value);
    EXPECT_THAT(pushed2, false);
    EXPECT_THAT(list.localEmpty(), false);
    EXPECT_THAT(list.localSize(), 1);
    EXPECT_THAT(list.sharedEmpty(), true);
}

TEST(StealableWorkListTest, ShareSome) {
    TestSubject list;
    auto values = range(0, 10);
    auto nodeHandle = fill(list, values);
    EXPECT_THAT(list.localEmpty(), false);
    EXPECT_THAT(list.localSize(), values.size());
    EXPECT_THAT(list.sharedEmpty(), true);
    auto sharedAmount = list.shareAll();
    EXPECT_THAT(sharedAmount, values.size());
    EXPECT_THAT(list.localEmpty(), true);
    EXPECT_THAT(list.sharedEmpty(), false);
}

TEST(StealableWorkListTest, TryStealFromEmpty) {
    TestSubject victim;
    TestSubject thief;
    auto stolenAmount = thief.tryStealFractionFrom(victim, 2);
    EXPECT_THAT(stolenAmount, 0);
}

TEST(StealableWorkListTest, TryStealHalf) {
    TestSubject victim;
    auto values = range(0, 10);
    auto nodeHandle = fill(victim, values);
    victim.shareAll();

    TestSubject thief;
    auto stolenAmount = thief.tryStealFractionFrom(victim, 2);
    EXPECT_THAT(stolenAmount, (values.size() + 1) / 2);
    EXPECT_THAT(thief.localSize(), stolenAmount);

    victim.tryStealFractionFrom(victim, 1);
    EXPECT_THAT(victim.sharedEmpty(), true);

    std_support::vector<int> allTheElements;
    drainLocalInto(victim, allTheElements);
    drainLocalInto(thief, allTheElements);
    EXPECT_THAT(allTheElements, testing::UnorderedElementsAreArray(values));
}

TEST(StealableWorkListTest, TryStealAllEventually) {
    TestSubject victim;
    auto values = range(0, 10);
    auto nodeHandle = fill(victim, values);
    victim.shareAll();

    TestSubject thief;
    for (std::size_t i = 0; i < values.size(); ++i) {
        auto stolenAmount = thief.tryStealFractionFrom(victim, 10000);
        EXPECT_THAT(stolenAmount, 1);
    }
    EXPECT_THAT(victim.sharedEmpty(), true);
    EXPECT_THAT(thief.localSize(), values.size());

    std_support::vector<int> allTheElements;
    drainLocalInto(victim, allTheElements);
    drainLocalInto(thief, allTheElements);
    EXPECT_THAT(allTheElements, testing::UnorderedElementsAreArray(values));
}

TEST(StealableWorkListTest, StealingPingPong) {
    TestSubject list1;
    TestSubject list2;
    auto values = range(0, 100);
    auto nodesHandle1 = fill(list1, values);
    auto nodesHandle2 = fill(list2, values);

    std::atomic ready = false;
    auto kIters = 10000;
    std_support::vector<ScopedThread> threads;
    for (int tIdx = 0; tIdx < 2; ++tIdx) {
        threads.emplace_back([&ready, kIters, tIdx, &list1, &list2] {
            TestSubject& self = tIdx % 2 == 0 ? list1 : list2;
            TestSubject& victim = tIdx % 2 == 0 ? list2 : list1;
            while (!ready.load()) {
                std::this_thread::yield();
            }
            for (int iter = 0; iter < kIters; ++iter) {
                if (!self.localEmpty()) self.shareAll();
                self.tryStealFractionFrom(victim, 2);
                if (!self.localEmpty()) self.shareAll();
                self.tryStealFractionFrom(victim, 1);
                if (auto popped = self.tryPopLocal()) {
                    popped->clearNext();
                    self.tryPushLocal(*popped);
                }
            }
        });
    }
    ready = true;

    for (auto& thr: threads) {
        thr.join();
    }

    // check nothing is lost
    list1.tryStealFractionFrom(list1, 1);
    list2.tryStealFractionFrom(list2, 1);
    std_support::vector<int> allTheElements;
    drainLocalInto(list1, allTheElements);
    drainLocalInto(list2, allTheElements);

    std_support::vector<int> expected;
    expected.insert(expected.end(), values.begin(), values.end());
    expected.insert(expected.end(), values.begin(), values.end());
    EXPECT_THAT(allTheElements, testing::UnorderedElementsAreArray(expected));
}
