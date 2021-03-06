(ns kuona-api.chat
  (:require [ring.util.response :refer [resource-response response status]]
            [kuona-api.core.store :as store]
            [kuona-api.core.stores :as stores]))

(defn message [msg]

  (response {:response
             {:message
              (let [text (-> msg :query)]
                (cond
                  (nil? text) "You need to ask a question..."
                  (re-matches #"How many repos are there" text) (str "There are " (-> (store/get-count stores/repositories-store) :count) " repositories currently loaded")
                  :else "Sorry I did not understand"
                  ))
              }})
  )
