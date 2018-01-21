(ns jenkins-collector.jenkins-test
  (:require [clj-http.client :as http]
            [midje.sweet :refer :all]
            [jenkins-collector.jenkins :refer :all]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log]
            [clojure.xml :as cxml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer :all]))

(def stubbed-home-response
  {:jobs
   [{:_class "hudson.model.FreeStyleProject",
     :name   "boost-program-options",
     :url    "http://jenkins.com/job/boost-program-options/",
     :color  "blue"}]})

(def stubbed-build-history
  {:builds [{:_class "hudson.model.FreeStyleBuild",
             :number 6,
             :url    "http://jenkins.com/job/intercept/6/"}
            {:_class "hudson.model.FreeStyleBuild",
             :number 5,
             :url    "http://jenkins.com/job/intercept/5/"}
            {:_class "hudson.model.FreeStyleBuild",
             :number 4,
             :url    "http://jenkins.com/job/intercept/4/"}
            {:_class "hudson.model.FreeStyleBuild",
             :number 0,
             :url    "http://jenkins.com/job/intercept/0/"}],
   :name "intercept"})


(def stubbed-build-result
  {:number    4,
   :duration  33835,
   :result    "SUCCESS",
   :id        "2013-12-20_12-42-32",
   :_class    "hudson.model.FreeStyleBuild",
   :executor  nil,
   :timestamp 1387543352000})

(defn stubbed-connection
  [url]
  (log/info "Stubbed request for " url)
  (case url
    "http://jenkins.com/" stubbed-home-response
    "http://jenkins.com/job/boost-program-options/" stubbed-build-history
    "http://jenkins.com/job/boost-program-options/4/" stubbed-build-result
    "http://jenkins.com/job/intercept/6/" stubbed-build-result
    (println "************************************************ No stubbed data" url)))


(facts "about job configuration reading"
       (let [empty-project            "<project></project>"
             project-with-description "<project><description>Project Description</description></project>"
             project-with-scm         "<project><scm class=\"hudson.plugins.git.GitSCM\" plugin=\"git@3.0.0\">
<configVersion>2</configVersion>
<userRemoteConfigs>
<hudson.plugins.git.UserRemoteConfig>
<url>git@github.com:kuona/dashboard.git</url>
</hudson.plugins.git.UserRemoteConfig>
</userRemoteConfigs>
<branches>
<hudson.plugins.git.BranchSpec>
<name>*/master</name>
</hudson.plugins.git.BranchSpec>
</branches>
<doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
<submoduleCfg class=\"list\"/>
<extensions/>
</scm></project>"]
         (fact "Empty XML document yields empty configuration"
               (zip-str empty-project) =>  [{:attrs nil, :content nil, :tag :project} nil])
         (fact "Extracts the project description"
               (read-description (zip-str project-with-description)) => "Project Description")
         (fact "Reads the source control details"
               (read-scm (zip-str project-with-scm)) => {:scm :git
                                                         :ref  "*/master"
                                                         :url  "git@github.com:kuona/dashboard.git"})))



(facts "about put-build!"
       (fact "posts build to api"
             (put-build! {} "http://server.com") => "worked"
             (provided
              (http/post "http://server.com/api/builds" {:headers {"content-type" "application/json; charset=UTF-8"}, :body "{}"}) => "worked")))

(facts "about upload-metrics"
       (fact "calls put-build for each metric"
             (upload-metrics '(1 2 3) "http://foo") => '(:result-1 :result-2 :result-3)
             (provided
              (put-build! 1 "http://foo") => :result-1
              (put-build! 2 "http://foo") => :result-2
              (put-build! 3 "http://foo") => :result-3)))

(facts "about jenkins auth"
       (fact "no auth if password missing"
             (http-credentials :username nil) => {})
       (fact "no auth if username missing"
             (http-credentials nil :password) => {})
       (fact "basic auth for username and password"
             (http-credentials "foo" "bar") => {:basic-auth ["foo" "bar"]}))

