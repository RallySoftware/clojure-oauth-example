(defproject rally-oauth-demo "0.1.0-SNAPSHOT"
  
  :description "Demo App using Rally Oauth"
  :url "http://github.com/RallySoftware/rally-oauth-demo"
  
  :license {:name "MIT License"}
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.3"]

                 [http-kit "2.1.16"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [ring/ring-core "1.2.1"]
                 [com.novemberain/monger "1.7.0"]
                 [cheshire "5.3.1"]

                 ]

  :uberjar-name "rally-oauth-demo-standalone.jar"
  :min-lein-version "2.0.0"

  )
