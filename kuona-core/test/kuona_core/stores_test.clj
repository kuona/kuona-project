(ns kuona-core.stores-test
  (:require [midje.sweet :refer :all]
            [kuona-core.stores :refer :all])
  (:import (kuona_core.stores DataStore)))

(facts "about indexes"
       (fact (es-index :foo {}) => {:name "foo"
                                    :type {}
                                    :url  "http://localhost:9200/foo"}))

(facts "about stores"
       (fact
         (DataStore. :idx :mapping-name {}) => (contains {:index-name :idx}))

       (fact (exists? (DataStore. :foo :bar {})) => false)
       (fact "url contains index and mapping names"
             (url (DataStore. :foo :bar {})) => "http://localhost:9200/kuona-foo/bar")
       (fact "url contains index and mapping names"
             (let [store (DataStore. :foo :bar {})]
               (url store) => "http://localhost:9200/kuona-foo/bar"
               (url store '(1)) => "http://localhost:9200/kuona-foo/bar/1"
               (url store [1]) => "http://localhost:9200/kuona-foo/bar/1"))
       (fact "handles parameters in url"
             (let [store (DataStore. :foo :bar {})]
               (url store [1] ["q=foo"]) => "http://localhost:9200/kuona-foo/bar/1?q=foo"
               (url store [1] ["q=foo" "r=bar"]) => "http://localhost:9200/kuona-foo/bar/1?q=foo&r=bar")))
