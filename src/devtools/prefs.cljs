(ns devtools.prefs
  (:require [clojure.string :as str]))

(def ^:const signature-color "rgba(100, 255, 100, 1);")

(defn signature-color-with-opacity [opacity]
  (str/replace signature-color "1);" (str opacity ");")))

(def signature-background (signature-color-with-opacity 0.08))
(def body-border-color (signature-color-with-opacity 0.4))

(def default-prefs
  {:install-custom-formatters        true                                                                                     ; the only feature enabled by default
   :install-sanity-hints             false
   :install-dirac-support            false
   :sanity-hint-min-length           128
   :max-print-level                  2
   :body-line-max-print-level        3
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
   :line-index-separator             ""
   :dq                               "\""
   :circular-reference-label         "∞"
   :circular-reference-wrapper-style ""
   :circular-reference-style         (str "background-color:#f88;"
                                          "color:#fff;"
                                          "opacity: 0.5;"
                                          "margin-right: 3px;"
                                          "padding: 0px 4px 0px 4px;"
                                          "border-radius:2px;"
                                          "-webkit-user-select: none;")
   :surrogate-key                    "$$this-is-cljs-devtools-surrogate"
   :standard-ol-style                (str "list-style-type:none;"
                                          "padding-left:0px;"
                                          "margin-top:0px;"
                                          "margin-bottom:0px;"
                                          "margin-left:0px")
   :standard-ol-no-margin-style      (str "list-style-type:none;"
                                          "padding-left:0px;"
                                          "margin-top:0px;"
                                          "margin-bottom:0px;"
                                          "margin-left:0px")
   :standard-li-style                "margin-left:0px"
   :standard-li-no-margin-style      "margin-left:0px"
   :spacer                           " "
   :span                             "span"
   :ol                               "ol"
   :li                               "li"
   :cljs-style                       (str "background-color:" signature-background ";")
   :header-style                     ""
   :item-style                       (str "display: inline-block;"
                                          "border-left: 2px solid rgba(100, 100, 100, 0.2);"
                                          "padding: 0px 4px 0px 4px;"
                                          "margin: 0px 0px 1px 0px;")
   :body-items-more-label-style      (str "background-color:#999;"
                                          "min-width: 50px;"
                                          "display: inline-block;"
                                          "color: #fff;"
                                          "cursor: pointer;"
                                          "line-height: 14px;"
                                          "font-size: 10px;"
                                          "border-radius:2px;"
                                          "padding: 0px 4px 0px 4px;"
                                          "margin: 1px 0px 0px 0px;"
                                          "-webkit-user-select: none;")
   :body-style                       (str "display:inline-block;"
                                          "padding: 14px 6px 0px 6px;"
                                          "border-top: 1px solid " body-border-color ";"
                                          "border-radius:1px;"
                                          "margin: -14px -2px 2px -2px;"
                                          "background-color:" signature-background ";")
   :index-style                      (str "min-width: 50px;"
                                          "display: inline-block;"
                                          "text-align: right;"
                                          "vertical-align: top;"
                                          "background-color:#ddd;"
                                          "color:#000;"
                                          "opacity: 0.5;"
                                          "margin-right: 3px;"
                                          "padding: 0px 4px 0px 4px;"
                                          "margin: 0px 0px 1px 0px;"
                                          "border-top-left-radius: 2px;"
                                          "border-bottom-left-radius: 2px;"
                                          "-webkit-user-select: none;")
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
   :meta-wrapper-style               (str "background-color:#efe;"
                                          "border:1px solid #ada;"
                                          "border-radius:2px;")
   :meta-style                       (str "background-color:#ada;"
                                          "color:#fff;"
                                          "padding:0px 2px 0px 4px;"                                                          ; border radius on :meta-wrapper-style adds another 2px to the right
                                          "-webkit-user-select: none;")
   :meta-body-style                  (str "border:1px solid #ada;"
                                          "position:relative;"
                                          "left:1px;"
                                          "top:-1px;"
                                          "margin-left:-1px;"
                                          "padding:1px;"
                                          "border-bottom-left-radius:2px;"
                                          "border-bottom-right-radius:2px;")
   :file-reader                      nil
   :header-pre-handler               nil
   :header-post-handelr              nil
   :has-body-pre-handler             nil
   :has-body-post-handler            nil
   :body-pre-handler                 nil
   :body-post-handler                nil
   :java-trace-header-style          "color:red"
   :dirac-print-level                1
   :dirac-print-length               10})

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