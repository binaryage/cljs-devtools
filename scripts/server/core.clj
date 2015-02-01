(ns server.core
  (:use ring.util.response)
  (:use ring.middleware.resource)
  (:use ring.middleware.content-type))

(defn handler [request]
  (-> (resource-response "index.html")
      (content-type "text/html")))

; Server
(def app
  (-> handler
      (wrap-resource "public")
      (wrap-resource "src")
      (wrap-content-type {:mime-types {"cljs" "text/plain"}})))
