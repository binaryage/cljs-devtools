(ns devtools.optional
  (:require [cljs.pprint]
            [cljs.stacktrace]))

; requires here do not play well with DCE in advanced mode
; see https://github.com/binaryage/cljs-devtools/issues/37
;
; this file exists to be required dynamically in dev mode only via
;   (emit-if-compiler-in-dev-mode (goog/require "devtools.optional"))
;
; we rely on the fact that in :none optimizations mode clojurescript compiler produces all js files
; even if they are not reachable from :main namespace (if specified)
;
; please note that you should manually require namespaces which these requires depend on
; to achieve correct ordering of requires for static code, currently goog.string and clojure.string

; -- wrappers ---------------------------------------------------------------------------------------------------------------
;
; these are convenience wrappers, cljs.stacktrace/parse-stacktrace is a multi-method, so we couldn't call it directly from js

(defn pprint [& args]
  (apply cljs.pprint/pprint args))

(defn parse-stacktrace [& args]
  (apply cljs.stacktrace/parse-stacktrace args))
