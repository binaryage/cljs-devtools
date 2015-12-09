(ns devtools-sample.server
  (:require [environ.core :refer [env]])
  (:use ring.util.response)
  (:use ring.middleware.resource)
  (:use ring.middleware.content-type))

(defn get-root-html-file []
  (if (env :devtools-debug)
    "public/debug.html"
    "public/index.html"))

(defn handler [request]
  (-> (resource-response (get-root-html-file))
      (content-type "text/html")))

(def cors-headers {"Access-Control-Allow-Origin"  "*"
                   "Access-Control-Allow-Headers" "Content-Type"
                   "Access-Control-Allow-Methods" "GET,POST,OPTIONS"})

(defn all-cors
  "Allow requests from all origins"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (update-in response [:headers]
                 merge cors-headers))))

(def app
  (-> handler
      (wrap-resource "public")
      (wrap-resource "src")
      (wrap-content-type {:mime-types {"cljs" "text/plain"
                                       "map"  "text/plain"}})
      (all-cors)))