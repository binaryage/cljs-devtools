(ns devtools.prefs
  (:require-macros [devtools.prefs :refer [symbol-style]]))

(def signature-background "rgba(100, 255, 100, 0.08)")
(def body-border-color "rgba(100, 255, 100, 0.4)")

(def string-background "rgba(255, 100, 100, 0.08)")
(def string-border-color "rgba(255, 100, 100, 0.4)")

(def body-line-common-style "")

(def default-prefs
  {:features-to-install                           :default
   :suppress-preload-install                      false
   :print-config-overrides                        false
   :sanity-hint-min-length                        128
   :max-print-level                               2
   :body-line-max-print-level                     3
   :max-header-elements                           5
   :seqables-always-expandable                    true
   :min-sequable-count-for-expansion              3
   :max-number-body-items                         100
   :string-prefix-limit                           20
   :string-postfix-limit                          20
   :more-marker                                   "…"
   :body-items-more-label                         "more…"
   :string-abbreviation-marker                    " … "
   :multi-arity-symbol                            "…"
   :well-known-types                              #{"cljs.core/PersistentVector"
                                                    "cljs.core/PersistentArrayMap"
                                                    "cljs.core/PersistentHashSet"
                                                    "cljs.core/PersistentHashMap"
                                                    "cljs.core/Range"
                                                    "cljs.core/LazySeq"}
   :max-instance-header-fields                    3
   :max-instance-custom-printing-level            2
   :max-list-protocols                            5
   :header-field-value-spacer                     #js ["span" #js {"style" "color: #aaa"} "="]
   :header-field-separator                        " "
   :header-field-name-style                       ""
   :header-protocol-separator                     " "
   :body-field-value-spacer                       #js ["span" #js {"style" "color: #aaa"} "= "]
   :body-field-symbol                             "∋"
   :body-field-symbol-style                       (symbol-style "#88f")
   :body-field-name-style                         ""
   :body-field-td1-style                          (str "vertical-align: top;"
                                                       "padding-right: 4px;")
   :more-fields-symbol                            "…"
   :instance-value-separator                      ""
   :instance-custom-printing-style                (str "border: 1px solid #aad;"
                                                       "border-left: none;"
                                                       "padding: 0 3px 0 4px;"
                                                       "position:relative; left:-1px;"
                                                       "border-radius: 0 2px 2px 0;")
   :instance-type-header-style                    (str "background-color: #aad;"
                                                       "color: #eee;"
                                                       "padding: 0px 2px 0px 2px;"
                                                       "border: 1px solid #aad;"
                                                       "border-radius: 2px 0 0 2px;"
                                                       "-webkit-user-select: none;")
   :instance-value-style                          (str "border: 1px solid #aad;"
                                                       "border-radius: 0 2px 2px 0;")
   :instance-body-fields-table-style              (str "border-spacing: 0;"
                                                       "border-collapse: collapse;")
   :fields-header-style                           (str "padding: 0px 3px;")
   :fields-header-open-symbol                     ""
   :fields-header-close-symbol                    ""
   :rest-symbol                                   " & "
   :args-open-symbol                              "["
   :args-close-symbol                             "]"
   :new-line-string-replacer                      "↵"
   :line-index-separator                          ""
   :dq                                            "\""
   :circular-reference-symbol                     "∞"
   :circular-reference-wrapper-style              ""
   :circular-reference-symbol-style               (symbol-style "#f88")
   :fn-header-style                               ""
   :fn-prefix-style                               ""
   :type-header-style                             (str "background-color: #aad;"
                                                       "color: #eee;"
                                                       "padding: 0px 2px 0px 2px;"
                                                       "border: 1px solid #aad;"
                                                       "border-radius: 2px;"
                                                       "-webkit-user-select: none;")
   :type-symbol                                   ""
   :type-symbol-style                             ""
   :type-name-style                               ""
   :type-basis-symbol                             "β"
   :type-basis-symbol-style                       (symbol-style "#228")
   :type-basis-style                              (str "color: #228;"
                                                       "margin-right:3px;")
   :instance-symbol                               "ω"
   :instance-symbol-style                         (symbol-style "#228")
   :protocols-list-symbol                         #js ["span" #js {"style" (symbol-style "#3aa")} "ρ"]
   :header-fast-protocol-style                    (str "background-color: #3aa;"
                                                       "color: #ffa;"
                                                       "margin-bottom: 1px;"
                                                       "padding: 0px 4px 0px 2px;"
                                                       "border-radius: 2px;"
                                                       "-webkit-user-select: none;")
   :header-slow-protocol-style                    (str "background-color: #3aa;"
                                                       "color: #eee;"
                                                       "margin-bottom: 1px;"
                                                       "padding: 0px 4px 0px 2px;"
                                                       "border-radius: 2px;"
                                                       "-webkit-user-select: none;")
   :more-protocols-symbol                         #js ["span" #js {"style" (str "background-color: #3aa;"
                                                                                "margin-left: 6px;"
                                                                                "color: #eee;"
                                                                                "margin-bottom: 1px;"
                                                                                "padding: 0px 4px;"
                                                                                "border-radius: 2px;"
                                                                                "-webkit-user-select: none;")} "…"]
   :protocol-ns-symbol                            "in"
   :protocol-ns-symbol-style                      (symbol-style "#999")
   :protocol-ns-name-style                        (str "color:#999")
   :protocol-native-symbol                        "js"
   :protocol-native-symbol-style                  (symbol-style "#990099")
   :max-protocol-method-arities-list              3
   :protocol-method-symbol                        #js ["span" #js {"style" (symbol-style "#3aa")} "m"]
   :protocol-method-name-style                    (str "margin-right: 6px;"
                                                       "color: #3aa")
   :protocol-method-arities-more-symbol           "…"
   :protocol-method-arities-list-header-separator " "
   :fn-symbol                                     "fn"
   :fn-symbol-style                               (symbol-style "#282")
   :fn-ns-symbol                                  "in"
   :fn-ns-symbol-style                            (symbol-style "#999")
   :fn-ns-name-style                              (str "color:#999")
   :fn-name-style                                 (str "color: #282;"
                                                       "margin-right:3px;")
   :fn-native-symbol                              "js"
   :fn-native-symbol-style                        (symbol-style "#990099")
   :fn-args-style                                 (str "color: #960;")
   :fn-multi-arity-args-indent-style              (str "visibility:hidden;"
                                                       "padding-left: 1px;")
   :fn-lambda-symbol                              "λ"
   :fn-lambda-symbol-style                        (symbol-style "#282")
   :standard-ol-style                             (str "list-style-type:none;"
                                                       "padding-left:0px;"
                                                       "margin-top:0px;"
                                                       "margin-bottom:0px;"
                                                       "margin-left:0px")
   :standard-ol-no-margin-style                   (str "list-style-type:none;"
                                                       "padding-left:0px;"
                                                       "margin-top:0px;"
                                                       "margin-bottom:0px;"
                                                       "margin-left:0px")
   :standard-li-style                             (str "margin-left:0px;"
                                                       body-line-common-style)
   :standard-li-no-margin-style                   (str "margin-left:0px;"
                                                       body-line-common-style)
   :aligned-li-style                              (str "margin-left:0px;"
                                                       body-line-common-style)
   :spacer                                        " "
   :span                                          "span"
   :div                                           "div"
   :ol                                            "ol"
   :li                                            "li"
   :cljs-style                                    (str "background-color:" signature-background ";")
   :header-style                                  "white-space: nowrap"                                                       ; this prevents jumping of content when expanding sections due to content wrapping
   :item-style                                    (str "display: inline-block;"
                                                       "white-space: nowrap;"
                                                       "border-left: 2px solid rgba(100, 100, 100, 0.2);"
                                                       "padding: 0px 4px 0px 4px;"
                                                       "margin: 1px 0px 0px 0px;")
   :body-items-more-label-style                   (str "background-color:#999;"
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
   :body-style                                    (str "display:inline-block;"
                                                       "padding: 3px 13px 3px 13px;"
                                                       "border-top: 1px solid " body-border-color ";"
                                                       "border-radius:1px;"
                                                       "margin: 0;"
                                                       "background-color:" signature-background ";")
   :index-style                                   (str "min-width: 50px;"
                                                       "display: inline-block;"
                                                       "text-align: right;"
                                                       "vertical-align: top;"
                                                       "background-color:#ddd;"
                                                       "color:#000;"
                                                       "opacity: 0.5;"
                                                       "margin-right: 3px;"
                                                       "padding: 0px 4px 0px 4px;"
                                                       "margin: 1px 0px 0px 0px;"
                                                       "-webkit-user-select: none;")
   :expanded-string-style                         (str "padding: 0px 12px 0px 12px;"
                                                       "color:#C41A16;"
                                                       "white-space: pre;"
                                                       "border-top: 1px solid " string-border-color ";"
                                                       "border-radius:1px;"
                                                       "margin: 0px 0px 2px 0px;"
                                                       "background-color:" string-background ";")
   :nil-style                                     "color:#808080"
   :nil-label                                     "nil"
   :keyword-style                                 "color:#881391"
   :integer-style                                 "color:#1C00CF"
   :float-style                                   "color:#1C88CF"
   :string-style                                  "color:#C41A16"
   :symbol-style                                  "color:#000000"
   :fn-style                                      "color:#090"
   :bool-style                                    "color:#099"
   :print-meta-data                               true
   :meta-wrapper-style                            (str "background-color:#efe;"
                                                       "border:1px solid #ada;"
                                                       "border-radius:2px;")
   :meta-style                                    (str "background-color:#ada;"
                                                       "color:#fff;"
                                                       "padding:0px 2px 0px 4px;"                                             ; border radius on :meta-wrapper-style adds another 2px to the right
                                                       "-webkit-user-select: none;")
   :meta-body-style                               (str "border:1px solid #ada;"
                                                       "position:relative;"
                                                       "left:1px;"
                                                       "top:-1px;"
                                                       "margin-left:-1px;"
                                                       "padding:1px;"
                                                       "padding-left: 14px;"
                                                       "border-bottom-left-radius:2px;"
                                                       "border-bottom-right-radius:2px;")
   :file-reader                                   nil
   :header-pre-handler                            nil
   :header-post-handelr                           nil
   :has-body-pre-handler                          nil
   :has-body-post-handler                         nil
   :body-pre-handler                              nil
   :body-post-handler                             nil
   :default-envelope-header                       "\uD83D\uDCE8"
   :default-envelope-style                        nil
   :default-envelope-tag                          "span"
   :bypass-availability-checks                    false})

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
