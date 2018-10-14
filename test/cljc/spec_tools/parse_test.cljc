(ns spec-tools.parse-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [spec-tools.parse :as parse]
            [spec-tools.core :as st]))

(s/def ::a string?)
(s/def ::b string?)
(s/def ::c string?)
(s/def ::d string?)
(s/def ::e string?)

(s/def ::f string?)
(s/def ::g string?)

(s/def ::keys (s/keys :opt [::e]
                      :opt-un [::e]
                      :req [::a (or ::b (and ::c ::d))]
                      :req-un [::a (or ::b (and ::c ::d))]))

(s/def ::keys2 (st/spec (s/keys :opt [::f]
                                :opt-un [::f]
                                :req [::g]
                                :req-un [::g])))

(s/def ::merged (s/merge ::keys ::keys2))

(deftest parse-test
  (testing "predicate"
    (is (= {:spec double?, :type :double}
           (parse/parse-spec 'clojure.core/double?)
           (parse/parse-spec double?))))
  (testing "s/nilable"
    (is (= {:spec int?
            :type :long,
            ::parse/nilable? true}
           (parse/parse-spec (s/nilable (s/nilable int?))))))
  (testing "s/or"
    (is (= {::parse/items [{:spec int?, :type :long} {:spec keyword?, :type :keyword}]
            :type [:or [:long :keyword]]}
           (parse/parse-spec (s/or :int int? :keyword keyword?)))))
  (testing "s/and"
    (is (= {::parse/items [{:spec int?, :type :long} {:spec keyword?, :type :keyword}]
            :type [:and [:long :keyword]]}
           (parse/parse-spec (s/and int? keyword?)))))
  (testing "s/keys"
    (is (= {:type :map
            ::parse/keys #{:a :b :c :d :e ::a ::b ::c ::d ::e}
            ::parse/keys-req #{:a :b :c :d ::a ::b ::c ::d}
            ::parse/keys-opt #{:e ::e}
            ::parse/key->spec {:a ::a
                               :b ::b
                               :c ::c
                               :d ::d
                               :e ::e
                               ::a ::a
                               ::b ::b
                               ::c ::c
                               ::d ::d
                               ::e ::e}}
           (parse/parse-spec ::keys)))

    (is (= {:type :map
            ::parse/keys #{:a :b :c :d :e :f :g ::a ::b ::c ::d ::e ::f ::g}
            ::parse/keys-req #{:a :b :c :d :g ::a ::b ::c ::d ::g}
            ::parse/keys-opt #{:e :f ::e ::f}
            ::parse/key->spec {:a ::a
                               :b ::b
                               :c ::c
                               :d ::d
                               :e ::e
                               :f ::f
                               :g ::g
                               ::a ::a
                               ::b ::b
                               ::c ::c
                               ::d ::d
                               ::e ::e
                               ::f ::f
                               ::g ::g}}
           (parse/parse-spec ::merged))))
  (testing "s/merge"
    (is (= {:type :map
            ::parse/keys #{:a :b}
            ::parse/keys-req #{:a :b}
            ::parse/key->spec {:a ::a, :b ::b}}
           (parse/parse-spec (s/merge (s/keys :req-un [::a]) (s/keys :req-un [::b]))))))
  (testing "s/every"
    (is (= {::parse/item {:spec int?, :type :long}
            :type :vector}
           (parse/parse-spec (s/every int?)))))
  (testing "s/coll-of"
    (is (= {::parse/item {:spec int?, :type :long}
            :type :vector}
           (parse/parse-spec (s/coll-of int?))))
    (is (= {::parse/item {:spec int?, :type :long}
            :type :set}
           (parse/parse-spec (s/coll-of int? :into #{}))))
    (is (= {::parse/item {::parse/items [:long :keyword]
                          ::parse/size 2
                          :type :vector}
            :type :map-of}
           (parse/parse-spec (s/coll-of (s/tuple int? keyword?) :into {})))))
  (testing "s/merge"
    (is (= {:type :map
            ::parse/keys #{:a :b}
            ::parse/keys-req #{:a :b}
            ::parse/key->spec {:a ::a, :b ::b}}
           (parse/parse-spec (st/merge (s/keys :req-un [::a]) (s/keys :req-un [::b])))))))
