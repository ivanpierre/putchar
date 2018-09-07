(ns api.cookie
  (:require [buddy.sign.compact :as buddy]
            [api.util :as util]
            [api.config :refer [config]]))

(defn sign [token]
  (buddy/sign token (:cookie-secret config)))

(defn unsign [cookie]
  (buddy/unsign cookie (:cookie-secret config)))

;; domain path expires
(defn token-cookie [value & {:keys [max-age path]
                             :or {path "/"
                                  max-age (* (* 3600 24) 30)}}]
  (let [xsrf-token (str (util/uuid))
        domain (if (util/production?)
                 ".putchar.org"
                 "")
        secure (if (util/production?)
                 true
                 false)]
    {"x" (cond->
           {:value   (sign value)
            :max-age max-age
            :http-only true
            :path path
            :secure secure}
           domain
           (assoc :domain domain))
     "xsrf-token" (cond->
                    {:value xsrf-token
                     :max-age max-age
                     :http-only true
                     :path "/"
                     :secure secure}
                    domain
                    (assoc :domain domain))}))

(def delete-token
  (let [domain (if (util/production?)
                 ".putchar.org"
                 "")]
    {"x" {:value ""
          :path "/"
          :expires "Thu, 01 Jan 1970 00:00:00 GMT"
          :http-only true
          :domain domain}
     "xsrf-token" {:value ""
                   :path "/"
                   :expires "Thu, 01 Jan 1970 00:00:00 GMT"
                   :http-only true
                   :domain domain}}))

(def setup-github-sync
  (let [domain (if (util/production?)
                 ".putchar.org"
                 "")]
    {"setup-github-sync" {:value "true"
                          :path "/"
                          :expires "Thu, 01 Jan 1970 00:00:00 GMT"
                          :domain domain}}))

(defn get-token [req]
  (when-let [access-token (get-in req [:cookies "x" :value])]
    (unsign access-token)))
