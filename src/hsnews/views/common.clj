(ns hsnews.views.common
  (:use [noir.core]
        [hiccup.page-helpers :only [include-css html5 link-to]]
        somnium.congomongo)
        (:use [somnium.congomongo.config :only [*mongo-config*]])
  (:require [clojure.string :as string]
            [noir.response :as resp]
            [noir.request :as req]
            [clj-time.format :as tform]
            [clj-time.core :as ctime]
            [clj-time.coerce :as coerce]
            [hsnews.models.user :as users]))



(pre-route "/*" {:keys [uri]}
           (when-not (or 
                       (users/current-user)
                       (= uri "/login")
                       (= uri "/sessions/create")
                       (re-find #"^/(css)|(img)|(js)|(favicon)" uri))
            (resp/redirect "/login")))

(def date-format (tform/formatter "MM/dd/yy" (ctime/default-time-zone)))

(defn user-link [username]
  (link-to (str "/users/" username) username))

(defpartial comment-item [{:keys [author ts body post_title post_id]}]
            [:li
             [:div.subtext
              [:span.author author]
              [:span.date (tform/unparse date-format (coerce/from-long ts))]
              [:span.postTitle "on: " (link-to (str "/posts/" (.toString post_id)) post_title)]]
             [:div.commentBody body]])


; TODO Make this function less horrible and inefficient.
; (no need for extra map over comments)
(defpartial comment-list [comments]
            (let [posts (fetch-by-ids :posts (map #(get % :post_id) comments))
                  posts-by-id (reduce #(assoc %1 (get %2 :_id) %2) {} posts)
                  comments (map #(assoc % :post_title (get (get posts-by-id (get % :post_id)) :title)) comments)]
              [:ol
               (map comment-item comments)]))

(defpartial error-text [errors]
            [:span.error (string/join " " errors)])

(defpartial layout [& content]
            (html5
              [:head
               [:title "Hacker School News"]
               (include-css "/css/style.css")]
              [:body
               [:div#wrapper
                [:header
                 (link-to "/" [:img.logo {:src "/img/hacker-school-logo.png"}])
                 [:h1#logo
                  (link-to "/" "Hacker School News")]
                 [:ul [:li (link-to "/" "new")]
                      [:li (link-to "/submit" "submit")]]
                 (let [username (users/current-user)]
                  (if username
                    [:div.user.loggedin
                      [:span.username (user-link username)]
                      (link-to "/logout" "log out")]
                    [:div.user.loggedout
                      (link-to {:class "register"} "/register" "register")
                      (link-to "/login" "log in")]))]
                [:div#content content]
                [:footer
                 [:ul
                  [:li (link-to "http://www.hackerschool.com" "Hacker School")]
                  [:li (link-to "https://github.com/nicholasbs/hsnews" "Source on Github")]]]]]))
