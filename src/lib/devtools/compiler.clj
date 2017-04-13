(ns devtools.compiler
  (:require [cljs.env]
            [devtools.prefs :refer [get-pref]]))

(def preloads-url "https://github.com/binaryage/cljs-devtools/blob/master/docs/installation.md#install-it-via-preloads")
(def issue-37-url "https://github.com/binaryage/cljs-devtools/issues/37")

(defn ^:dynamic make-optimizations-warning-msg [mode]
  (str "WARNING: You required cljs-devtools library in a project which is currently compiled with :optimizations " mode ".\n"
       "         You should remove this library from non-dev builds completely because it impedes dead code elimination.\n"
       "         The best way is to use :preloads compiler option: " preloads-url ".\n"
       "         To silence this warning please set :silence-optimizations-warning config key to true.\n"
       "         More details: " issue-37-url "."))

(defn get-compiler-optimizations-mode []
  (or (if cljs.env/*compiler*
        (get-in @cljs.env/*compiler* [:options :optimizations]))
      :none))

(defn compiler-in-dev-mode? []
  (= (get-compiler-optimizations-mode) :none))

(defn emit-compiler-warning! [msg]
  (binding [*out* *err*]
    (println msg)))

(def emit-compiler-warning-once! (memoize emit-compiler-warning!))

(defn warn-if-optimizations! []
  (if-not (compiler-in-dev-mode?)
    (emit-compiler-warning-once! (make-optimizations-warning-msg (get-compiler-optimizations-mode)))))

(defn silence-optimizations-warnings? []
  (true? (get-pref :silence-optimizations-warning)))

(defmacro check-compiler-options! []
  (if-not (silence-optimizations-warnings?)
    (warn-if-optimizations!))
  nil)
