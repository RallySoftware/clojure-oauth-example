(ns ^{:doc "This namespace handles most of the oauth logic for
  authenticating a user via Rally's Oauth2 server, using the
  Authorization Code Grant mechanism.
  See: http://tools.ietf.org/html/rfc6749#section-4.1 "}
  rally-oauth-demo.oauth
  (:require [clojure.core.incubator :refer [dissoc-in]]
            [cheshire.core :as json]
            [org.httpkit.client :as client]
            [hiccup.core :refer [html]]
            [ring.util.response :as response]
            [rally-oauth-demo.config :refer [config]]
            [rally-oauth-demo.rally :as rally]
            [rally-oauth-demo.ui :as ui]
            [rally-oauth-demo.util :as util])
  (:import [java.util UUID]))


;;; Dummy implementation of logging, just to keep this demo simple
(def log println)


;;; You can override where the Rally Oauth server is, although I can't
;;; think of a reason why you would need to.
(def rally-oauth-server (config "OAUTH_SERVER" "https://rally1.rallydev.com/login/oauth2"))

;;; These are the URLs for Rally's Oauth endpoints.
;;; They are documented at: http://developer.rallydev.com/??? FIXME
(def rally-auth-url     (str rally-oauth-server "/auth"))
(def rally-token-url    (str rally-oauth-server "/token"))

;;; This app's client credentials, obtained from Rally. The client_id
;;; is publicly visible, but you must keep the client_secret a secret
;;; (but you could have guessed that from the name, right?)
(def client-id          (config "CLIENT_ID"))
(def client-secret      (config "CLIENT_SECRET"))


(defn login
  "This request handler initiates the Oauth2 login process by
  redirecting the end-user to Rally's Oauth authorization endpoint
  with the appropriate URL paramaters.

  We do two things here:
  1) Store a unique \"state\" token in the user's session.
  2) Redirect the end-user to Rally's Oauth server.

  See: http://tools.ietf.org/html/rfc6749#section-4.1.1
  for a full explanation of what is happening here.
  "
  [request]
  (let [state    (str (UUID/randomUUID))

        ;; We need the URL to this server, so that we can build the redirect_uri
        ;; parameter to be sent to Rally's authorization endpoint.
        server   (util/server-url request)

        ;; URL parameters to be sent to Rally's /auth endpoint
        params   {
                  ;; The client_id that was issued to us by Rally's Oauth server.
                  :client_id     client-id

                  ;; Only "code" is supported.
                  :response_type "code"

                  ;; This is the URI that users will be sent to once they have
                  ;; authorized (or denied) this app to use the Rally API on
                  ;; their behalf. This URL must be registered with Rally's
                  ;; Oauth server as being valid for this client_id.
                  :redirect_uri  (str server "/oauth-redirect")

                  ;; This is a unique parameter used for CSRF protection.
                  :state         state

                  ;; This indicates the level of privilege that our app is
                  ;; requesting. See: http://developer.rallydev.com/??? FIXME
                  :scope         "openid"
                  }

        auth-url (str rally-auth-url "?" (util/query-string params))]

    (-> auth-url
        response/redirect
        (assoc-in [:session :oauth-state] state))))


(defn- sad-panda
  "Helper function to return an (ugly) HTML page that displays an error
  indicating authorization failure."
  ([request]
     (sad-panda request "generic error"))
  ([request error]
     (ui/layout
      request
      [:div
       ;; For our purposes, there are only three classes of errors.
       ;; See: http://tools.ietf.org/html/rfc6749#section-4.1.2.1
       ;; for this list of defined error codes.
       (case error

         ;; The end user did not grant the access that we requested.
         "access_denied"
         (str "You did not grant us access to your Rally account. "
              "We are sad. All your base are not belong to us.")

         ;; Rally's Oauth server was not able to respond right now
         "temporarily_unavailable"
         (str "Rally's Oauth server is temporarily unavailable. "
              "Please try again shortly.")

         ;; Any other error code probably indicates a programming error by us - we
         ;; sent bad parameters of some sort. Hopefully this only happens during
         ;; development, and not in production.
         (do
           (log "Unhandled error: " error)
           (str "Something went wrong, and we could not access your Rally account. "
                "Error code was: " error)))])))


(declare get-access-token)

(defn oauth-redirect
  "This request handler will be invoked when the end-user has completed the
  authorization with Rally by either authorizing or denying our app.

  We arrive here because the Rally Oauth server redirected the end-user to this
  URL, with either a \"code\" or an \"error\" appended to the query-string.

  See: http://tools.ietf.org/html/rfc6749#section-4.1.2.1
  "
  [request]
  (let [{:strs [code state error]} (:query-params request)]
    (->
     (cond

      ;; If the "error" parameter is in the URL, something went wrong. Display an
      ;; appropriate message to the end-user.
      error
      (sad-panda request error)

      ;; If the state parameter sent back to us does not match the one in our
      ;; session, then something fishy is going on.
      (not= state (get-in request [:session :oauth-state]))
      (do
        (log "Mismatched state. Got: " state
             ", but session has: " (get-in request [:session :oauth-state]))
        (sad-panda request "Mismatched state parameter."))

      ;; Otherwise, we can now move on to phase two of the authorization flow,
      ;; where we cash in the code for an access_token.
      :else
      (get-access-token request))

     ;; Regardless of what we are returning, we should now discard the
     ;; :oauth-state from the user's session
     (dissoc-in [:session :oauth-state]))))


(defn- get-access-token
  "This method performs the access-token request to Rally's Oauth server.

  See: http://tools.ietf.org/html/rfc6749#section-4.1.3
  "
  [request]
  (let [code     (get-in request [:query-params "code"])
        server   (util/server-url request)

        ;; POST to the Rally Oauth server's "token" endpoint. We are using
        ;; http-kit's HTTP client, but any HTTP client library should work.
        response @(client/post
                   (str rally-oauth-server "/token")
                   {
                    ;; These are the required parameters in the form body:
                    :form-params {
                                  ;; The code that we received from the oauth
                                  ;; server
                                  "code"         code

                                  ;; The exact same redirect_uri that was used as
                                  ;; part of the authorization request, above.
                                  "redirect_uri" (str server "/oauth-redirect")

                                  ;; "grant_type", which must be
                                  ;; "authorization_code"
                                  "grant_type"   "authorization_code"}

                    ;; This app's client_id and client_secret, sent in a Basic
                    ;; Auth header. Note that Rally also supports sending these in
                    ;; the form body, but the Auth header is the recommended way.
                    :basic-auth [client-id client-secret]
                    })]

    ;; The response *should* contain JSON, as described here:
    ;; http://tools.ietf.org/html/rfc6749#section-4.1.4
    (let [{:keys [status headers body error]} response
          body (json/decode body true)]
      (if (not= status 200)
        (let [{:keys [error error_description error_uri]} body]
          ;; Something went wrong...
          ;; We could certainly do some more robust error handling here, but
          ;; this will do for a demo.
          (log "Error repsone from token endpoint:" body)
          (sad-panda request))
        
        ;; Otherwise - Great Success!
        (let [{:keys [access_token token_type expires_in refresh_token]} body]
          
          ;; Currently, the best way to get information about the user
          ;; is to call the Rally web-services API. In the future, we
          ;; will be adding the ability to get basic user-info
          ;; directly from the Oauth server.
          (if-let [user (rally/user-info access_token)]
            ;; Associate the returned information with the user's session.  In a
            ;; "real" application, we would store this stuff in a database.
            (-> (response/redirect (get-in request [:session :goto-after-login] "/"))
                (update-in [:session] merge {:oauth body
                                             :user  user}))

            ;; Failed to get user info.
            {:status 500 :body "Failed to get user info :("}
            ))))))


(defn logout [request]
  (-> (response/redirect "/")
      (assoc :session nil)))
