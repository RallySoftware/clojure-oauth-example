(ns rally-oauth-demo.ui
  (:require [hiccup.core :refer [html]]
            [rally-oauth-demo.rally :as rally]))


(defn bootstrap []
  (list 
        ))

(defn layout [request content]
  (let [{:keys [user]} (:session request)]
    (html
     [:head
      [:title "Rally Oauth Demo App"]
      [:link {:rel "stylesheet" :href "//netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css"}]]
     
     [:body
      [:div.navbar.navbar-inverse.navbar-fixed-top
       [:div.container
        [:div.navbar-header
         [:a.navbar-brand {:href "/"} "Rally Oauth Demo App"]]
        [:div.collapse.navbar-collapse]]]

      [:div.container {:style "margin-top: 50px"}
       [:div.starter-template
        [:h1 "Rally Oauth Demo App"]
        [:p.lead
         
         content]]]

      [:script {:src "//code.jquery.com/jquery.js"}]
      [:script {:src "//netdna.bootstrapcdn.com/bootstrap/3.1.0/js/bootstrap.min.js"}]
      ])))

(defn landing-page [request]
  (let [username (get-in request [:session :user "UserName"])]
    (layout
     request
     (if username
       [:div
        [:p "You are logged in as Rally user " username]
        [:ul
         [:li [:a {:href "/app/stories"} "My Stories"]]
         [:li [:a {:href "/logout"} "Log out"]]]]
       [:div
        [:p "You can log in to this application with your Rally account."]
        [:a {:href "/login"} "Log in using Rally"]]))))

(defn stories [request]
  (let [username     (get-in request [:session :user "UserName"])
        access-token (get-in request [:session :oauth :access_token])
        stories      (rally/query access-token
                                  "/hierarchicalrequirement"
                                  {:fetch "Name"
                                   :query (str "(Owner = " username ")")})]
    (layout
     request
     [:ul
      (for [{:strs [Name _refObjectUUID] :as story} stories]
        [:li [:a {:href (str "/app/story/" _refObjectUUID)} Name]])])))
