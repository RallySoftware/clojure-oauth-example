(ns rally-oauth-demo.main
  (:require [rally-oauth-demo.server])
  (:gen-class))

(defn -main [& args]
  (rally-oauth-demo.server/start))
