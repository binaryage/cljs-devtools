(ns devtools.prefs)

(def default-prefs
  {:max-print-level             2
   :max-header-elements         5
   :max-number-body-items       100
   :string-prefix-limit         20
   :string-postfix-limit        20
   :more-marker                 "…"
   :body-items-more-label       "more…"
   :string-abbreviation-marker  " … "
   :new-line-string-replacer    "↵"
   :line-index-separator        ":"
   :dq                          "\""
   :surrogate-key               "$$this-is-cljs-devtools-surrogate"
   :standard-ol-style           "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:12px"
   :standard-ol-no-margin-style "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:0px"
   :standard-li-style           "margin-left:12px"
   :standard-li-no-margin-style "margin-left:0px"
   :spacer                      " "
   :span                        "span"
   :ol                          "ol"
   :li                          "li"
   :cljs-style                  "background-color:#efe"
   :index-style                 "color:#881391"
   :nil-style                   "color:#808080"
   :nil-label                   "nil"
   :keyword-style               "color:#881391"
   :integer-style               "color:#1C00CF"
   :float-style                 "color:#1C88CF"
   :string-style                "color:#C41A16"
   :symbol-style                "color:#000000"
   :fn-style                    "color:#090"
   :bool-style                  "color:#099"})

(def ^:dynamic *prefs* default-prefs)

(defn set-prefs! [new-prefs]
  (set! *prefs* new-prefs))

(defn get-prefs []
  *prefs*)

(defn pref [k]
  (k *prefs*))