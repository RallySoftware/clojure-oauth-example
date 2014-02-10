(ns rally-oauth-demo.rally
  (:require [org.httpkit.client :as client]
            [cheshire.core :as json]
            [rally-oauth-demo.util :as util]))


(def rally-wsapi-url "https://rally1.rallydev.com/slm/webservice/v2.x")

(defn rally-get
  "Make an HTTP GET call to the Rally WSAPI service."
  ([access-token path]
     (rally-get access-token path nil))
  ([access-token path params]
     (let [url (util/url (str rally-wsapi-url path) params)
           response
           @(client/get url
                        {:headers {"zsessionid" access-token}})
           {:keys [status headers body error]} response]
       (if error
         (throw error)                  ; TODO - handle expired access_token
         (json/decode body)))))

(defn user-info
  "Call WSAPI to get info about the user that the access-token belongs to."
  [access-token]
  (get (rally-get access-token "/user") "User"))

(defn query
  "Do a WSAPI query and return a lazy seq of the results."
  ([access-token path params]
     (query access-token path params 1))
  ([access-token path params page-number]
     (let [params   (assoc params
                      :pagesize 20
                      :start    (* 20 (dec page-number)))
           response (get (rally-get access-token path params) "QueryResult")
           results  (get response "Results")]
       (when-not (empty? results)
         (concat results (query access-token path params (inc page-number)))))))
