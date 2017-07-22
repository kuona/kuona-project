(ns environment-service.handler-test
  (:require [ring.mock.request :as mock]
            [cheshire.core :refer :all]
            [midje.sweet :refer :all]
            [environment-service.handler :refer :all]
            [environment-service.environments :refer :all]))

(defn mock-json-post
  [app url body]
  (app (-> (mock/request :post url
                         (generate-string body))
           (mock/content-type "application/json"))))

(defn parse-json-response
  [response]
  (try
    (parse-string (:body response) true)
    (catch Exception _
        {})))

(future-facts "about route mapping"
       (with-state-changes [(before :facts (set-and-initialize-index "kuona-test-env1"))]
         (fact
          (let [response (app (mock/request :get "/"))]
            (:status response) => 200
            (parse-json-response response) => { :links [{:href "/api/environments" :rel "environments"}]}))
         
         (fact
          (let [response (app (mock/request :get "/api/environments"))]
            (:status response) => 200))

         (fact
          (let [response (mock-json-post app "/api/environments" { :environment { :name "TEST1"}})]
            (:status response) => 200))

         (fact
          (let [response (mock-json-post app "/api/environments"{ :environment { :name "TEST1"  :comment {:assessment "Unavailable" :message "@graham Setting up test data"}  }})]
            (:status response) => 200))

         
         (fact
          (let [response (app (mock/request :get "/api/environments/TEST1"))]
            (:status response) => 200))

         (fact
          (parse-json-response (app (mock/request :get "/api/environments/TESTING"))) => {:environment {:name "TESTING"},
                                                                                          :links [{:href "/api/environments/TESTING", :rel "self"}
                                                                                                  {:href "/api/environments/TESTING/comments", :rel "comments"}]}
          (provided
           (get-environment anything) => { :name "TESTING"}))

         (fact
          (let [comment-response (mock-json-post app "/api/environments/TEST1/comments" { :comment {:assessment "Available" :message "@graham Everything is good"}})]
            (:status comment-response) => 200))

         (fact
          (let [comment-response (mock-json-post app "/api/environments/TEST1/comments" { :comment {:assessment "Available"
                                                                                                    :message "@graham Everything is pretty good bla bla"}})
                response (app (mock/request :get "/api/environments/TEST1"))
                body (parse-json-response comment-response)
                updated-body (parse-json-response response)
                c (:comment (:environment updated-body))]
            (:assessment c) =>  "Available"
            (:message c) => "@graham Everything is pretty good bla bla"
            (:tags c) => (contains ["ENVIRONMENT" "TEST1"])))

         (fact "version is independently updatable"
          (let [response (mock-json-post app "/api/environments/TEST1/version" { :version "1.1.2" })
                updated-body (parse-json-response response)
                v (:version (:environment updated-body))]
            (:status response) => 200
            v => "1.1.2"))
         (fact "status is independently updatable"
          (let [response (mock-json-post app "/api/environments/TEST1/status" { :status "UP" })
                updated-body (parse-json-response response)
                v (:status (:environment updated-body))]
            (:status response) => 200
            v => "UP"))

         (fact
          (let [result (parse-json-response (app (mock/request :get "/api/environments/TEST1/comments")))]
            (:tags (first result))=> (contains ["ENVIRONMENT" "TEST1"])))

         (fact
          (update-environment-mapping) => {:acknowledged true})

         (fact
          (let [response (app (mock/request :get "/api/environments/MISSING"))]
            (:status response) => 404))

         (fact
          (let [response (app (mock/request :get "/invalid"))]
            (:status response) => 404))))