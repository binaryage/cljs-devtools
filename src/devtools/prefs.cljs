(ns devtools.prefs
  (:require [clojure.string :as str]))

(def ^:const signature-color "rgba(100, 255, 100, 1);")

(defn signature-color-with-opacity [opacity]
  (str/replace signature-color "1);" (str opacity ");")))

(def signature-background (signature-color-with-opacity 0.08))

(def default-prefs
  {:install-sanity-hints             false
   :sanity-hint-min-length           128
   :max-print-level                  2
   :max-header-elements              5
   :seqables-always-expandable       true
   :min-sequable-count-for-expansion 3
   :max-number-body-items            100
   :string-prefix-limit              20
   :string-postfix-limit             20
   :more-marker                      "…"
   :body-items-more-label            "more…"
   :string-abbreviation-marker       " … "
   :new-line-string-replacer         "↵"
   :line-index-separator             ":"
   :dq                               "\""
   :surrogate-key                    "$$this-is-cljs-devtools-surrogate"
   :standard-ol-style                "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:12px"
   :standard-ol-no-margin-style      "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:0px"
   :standard-li-style                "margin-left:12px"
   :standard-li-no-margin-style      "margin-left:0px"
   :spacer                           " "
   :span                             "span"
   :ol                               "ol"
   :li                               "li"
   :cljs-style                       (str "background-color:" signature-background)
   :header-style                     ""
   :item-style                       ""
   :more-style                       "background-color:#999; color:#fff; padding:0px 2px 0px 2px;"
   :body-style                       (str "display:inline-block;border: 1px dashed " (signature-color-with-opacity 0.4) ";border-top:none;border-radius:1px;margin: 0px 4px 2px 4px;background-color:" signature-background)
   :index-style                      "color:#881391"
   :nil-style                        "color:#808080"
   :nil-label                        "nil"
   :keyword-style                    "color:#881391"
   :integer-style                    "color:#1C00CF"
   :float-style                      "color:#1C88CF"
   :string-style                     "color:#C41A16"
   :symbol-style                     "color:#000000"
   :fn-style                         "color:#090"
   :bool-style                       "color:#099"
   :print-meta-data                  true
   :meta-wrapper-style               "background-color:#efe; border:1px solid #ada; border-radius:2px;"
   :meta-style                       "background-color:#ada; color:#fff; padding:0px 2px 0px 4px;"                    ; border radius on :meta-wrapper-style adds another 2px to the right
   :file-reader                      nil
   :header-pre-handler               nil
   :header-post-handelr              nil
   :has-body-pre-handler             nil
   :has-body-post-handler            nil
   :body-pre-handler                 nil
   :body-post-handler                nil})

(def ^:dynamic *prefs* default-prefs)

(defn get-prefs []
  *prefs*)

(defn pref [key]
  (key *prefs*))

(defn set-prefs! [new-prefs]
  (set! *prefs* new-prefs))

(defn set-pref! [key val]
  (set-prefs! (assoc (get-prefs) key val)))

(defn merge-prefs! [m]
  (set-prefs! (merge (get-prefs) m)))

(defn update-pref! [key f & args]
  (let [new-val (apply f (pref key) args)]
    (set-pref! key new-val)))