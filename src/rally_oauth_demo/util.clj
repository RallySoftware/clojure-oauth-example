(ns rally-oauth-demo.util
  (:require [ring.util.codec :as codec]))


(defn query-string
  "Helper function to build a URL query-string."
  [params]
  (->> params
       (remove (comp nil? second))
       (map (fn [[k v]] (str (codec/url-encode (name k))
                             \=
                             (codec/url-encode v))))
       (interpose \&)
       (apply str)
       not-empty))

(defn url
  "Helper function to build a url."
  [url params]
  (let [q-string (query-string params)]
    (str url (when q-string "?") q-string)))

(defn server-url
  "Helper function to rebuild this server's URL from a ring request."
  [{:keys [scheme server-name server-port]}]
  (str (name scheme) "://" server-name (when (not= 80 server-port) (str \: server-port))))


