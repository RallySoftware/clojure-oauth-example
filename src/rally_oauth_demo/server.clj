(ns rally-oauth-demo.server
  (:require [org.httpkit.server :as server]
            [rally-oauth-demo.routes :as routes]
            [rally-oauth-demo.config :as config]))


(defn start []
  (config/require! "CLIENT_ID" "CLIENT_SECRET")
  (let [port (Long/valueOf (config/config "PORT" 8777))]
    (server/run-server #'routes/handler
                       {:port   port
                        :thread 128})))
