(ns rally-oauth-demo.routes
  (:require [clojure.java.io :as io]
            [clojure.pprint]
            [compojure.core :refer [GET PUT POST routes defroutes context]]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware
             [cookies :refer [wrap-cookies]]
             [params  :refer [wrap-params]]
             [session :refer [wrap-session]]]
            [ring.util.response :as response]
            [rally-oauth-demo.ui :as ui]
            [rally-oauth-demo.oauth :as oauth]))


(defn wrap-require-authentication [handler]
  (fn [request]
    (if (get-in request [:session :user])
      (handler request)
      (-> (response/redirect "/login")
          (assoc-in [:session :goto-after-login] (:uri request))))))

(def authenticated-routes
  (wrap-require-authentication
   (routes
    (GET "/stories" []   [] ui/stories))))

(defroutes my-routes
  (GET "/"               [] ui/landing-page)

  ;; Oauth routes
  (GET "/login"          [] oauth/login)
  (GET "/oauth-redirect" [] oauth/oauth-redirect)

  ;; Logout (yeah, this really shouldn't be a GET...)
  (GET "/logout"         [] oauth/logout)

  (context "/app" []
   authenticated-routes)
  
  (route/resources "/")
  (route/not-found "Not Found"))


(defn wrap-debug [handler]
  (fn [request]
    (println "REQUEST ====================")
    (clojure.pprint/pprint request)
    (let [response (handler request)]
      (println "RESPONSE ===================")
      (clojure.pprint/pprint response)
      (println)
      response)))

(def handler
  (-> my-routes
      ;;wrap-debug
      wrap-session
      wrap-params
      wrap-cookies
      ))
