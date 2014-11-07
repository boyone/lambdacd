(ns lambdacd.util-test
  (:use [lambdacd.util])
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async]))

(deftest range-test
  (testing "that range produces a range from a value+1 with a defined length"
    ; TODO: the plus-one is like that because the user wants it, probably shouldn't be like this..
    (is (= '(6 7 8) (range-from 5 3)))))

(defn some-function [] {})

(deftest is-channel-test
  (testing "that it can detect if something is a channel"
    (is (= true (is-channel? (async/chan))))
    (is (= false (is-channel? :foo)))
    (is (= false (is-channel? "hello")))
    (is (= false (is-channel? some-function)))))