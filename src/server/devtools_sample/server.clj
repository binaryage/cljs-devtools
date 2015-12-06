(ns devtools-sample.server
  (:require [environ.core :refer [env]])
  (:use ring.util.response)
  (:use ring.middleware.resource)
  (:use ring.middleware.refresh)
  (:use ring.middleware.content-type))

(defn get-root-html-file []
  (if (env :devtools-debug)
    "debug.html"
    "index.html"))

(defn handler [request]
  (-> (resource-response (get-root-html-file))
      (content-type "text/html")))

(def app
  (-> handler
      (wrap-resource "public")
      (wrap-resource "src")
      (wrap-content-type {:mime-types {"cljs" "text/plain"}})
      (wrap-refresh ["src" "_compiled"])))
