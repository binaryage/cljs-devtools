(ns devtools.defaults
  (:require-macros [devtools.prefs :as p]))

(def prefs
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

   :basis-icon                                    (p/icon "β" (p/get-type-color))
   :protocols-icon                                (p/icon "⊢" (p/get-protocol-color))
   :fields-icon                                   (p/icon "∋" (p/get-type-color))
   :method-icon                                   (p/icon "m" (p/get-method-color))
   :ns-icon                                       (p/icon "in" (p/get-ns-color))
   :native-icon                                   (p/icon "js" (p/get-native-color))
   :lambda-icon                                   (p/icon "λ" (p/get-lambda-color))
   :fn-icon                                       (p/icon "fn" (p/get-fn-color))
   :circular-ref-icon                             (p/icon "∞" (p/get-circular-ref-color) :slim)

   :header-field-value-spacer                     #js ["span" #js {"style" "color: #ccc"} "="]
   :header-field-separator                        " "
   :header-field-name-style                       ""
   :body-field-value-spacer                       #js ["span" #js {"style" "color: #ccc"} "= "]
   :body-field-td1-style                          (str "vertical-align: top;"
                                                       "padding:0;"
                                                       "padding-right: 4px;")
   :body-field-td2-style                          (str "vertical-align: top;"
                                                       "padding:0;")
   :more-fields-symbol                            "…"
   :instance-header-style                         (str (p/type-outline-style))
   :standalone-type-style                         (str (p/type-outline-style))
   :instance-value-separator                      ""
   :instance-custom-printing-style                (str "position: relative;"
                                                       "padding: 0 2px 0 4px;")
   :instance-custom-printing-wrapper-style        (str "position: relative;"
                                                       "border-radius:2px;")
   :instance-custom-printing-background           #js ["span" #js {"style" (p/get-custom-printing-background)} ""]
   :type-header-background                        #js ["span" #js {"style" (p/get-instance-type-header-background)} ""]
   :instance-type-header-style                    (str (p/get-common-type-header-style)
                                                       "border-radius: 2px 0 0 2px;")
   :instance-body-fields-table-style              (str "border-spacing: 0;"
                                                       "border-collapse: collapse;"
                                                       "margin-bottom: -2px;"                                                 ; weird spacing workaround
                                                       "display: inline-block;")
   :fields-header-style                           (str "padding: 0px 3px;")
   :fields-header-open-symbol                     ""
   :fields-header-close-symbol                    ""
   :rest-symbol                                   " & "
   :args-open-symbol                              "["
   :args-close-symbol                             "]"
   :new-line-string-replacer                      "↵"
   :line-index-separator                          ""
   :dq                                            "\""
   :fn-header-style                               ""
   :fn-prefix-style                               ""
   :native-reference-background                   #js ["span" #js {"style" (p/get-native-reference-background)} ""]
   :native-reference-wrapper-style                (str "display: inline-block;"                                               ; a hacky correction to hairy devtools tree-outline.css styles
                                                       "padding: 0px 3px;"
                                                       "position: relative;"
                                                       "margin-bottom: -3px;"
                                                       "top: -3px;")
   :type-wrapper-style                            (str "position: relative;"
                                                       "border-radius:2px;")
   :type-ref-style                                (str "position:relative;")
   :type-header-style                             (str (p/get-common-type-header-style)
                                                       "border-radius: 2px;")
   :type-name-style                               (str "padding-right: 4px;")
   :type-basis-style                              (str "margin-right:3px;")
   :type-basis-item-style                         (str "color: #228;"
                                                       "margin-right: 6px;")
   :protocol-background                           #js ["span" #js {"style" (p/get-protocol-background)} ""]
   :protocol-name-style                           (str "position: relative;")
   :fast-protocol-style                           (str (p/get-common-protocol-style)
                                                       "color: #ffa;")
   :slow-protocol-style                           (str (p/get-common-protocol-style)
                                                       "color: #eee;")
   :protocol-more-style                           (str "font-size: 8px;"
                                                       "position: relative;")
   :protocol-ns-name-style                        (str "color:" (p/get-ns-color) ";")
   :max-protocol-method-arities-list              3
   :protocol-method-name-style                    (str "margin-right: 6px;"
                                                       "color: #3aa")
   :protocol-method-arities-more-symbol           "…"
   :protocol-method-arities-list-header-separator " "
   :fn-ns-name-style                              (str "color:" (p/get-ns-color) ";")
   :fn-name-style                                 (str "color: " (p/get-fn-color) ";"
                                                       "margin-right:3px;")
   :fn-args-style                                 (str "color: #960;")
   :fn-multi-arity-args-indent-style              (str "visibility:hidden;"
                                                       "padding-left: 1px;")
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
                                                       (p/get-body-line-common-style))
   :standard-li-no-margin-style                   (str "margin-left:0px;"
                                                       (p/get-body-line-common-style))
   :aligned-li-style                              (str "margin-left:0px;"
                                                       (p/get-body-line-common-style))
   :spacer                                        " "
   :span                                          "span"
   :div                                           "div"
   :ol                                            "ol"
   :li                                            "li"
   :table                                         "table"
   :td                                            "td"
   :tr                                            "tr"
   :cljs-land-style                               (str "background-color:" (p/get-signature-background-color) ";"
                                                       "border-radius: 2px;")
   :header-style                                  "white-space: nowrap"                                                       ; this prevents jumping of content when expanding sections due to content wrapping
   :item-style                                    (str "display: inline-block;"
                                                       "white-space: nowrap;"
                                                       "border-left: 2px solid rgba(100, 100, 100, 0.2);"
                                                       "padding: 0px 4px 0px 4px;"
                                                       "margin: 1px 0px 0px 0px;")
   :body-more-style                               (str "background-color:#999;"
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
                                                       "padding: 3px 11px 3px 11px;"
                                                       "border-top: 1px solid " (p/get-body-border-color) ";"
                                                       "border-radius:1px;"
                                                       "margin: 1px;"
                                                       "margin-top: -1px;"
                                                       "background-color:" (p/get-signature-background-color) ";")
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
                                                       "border-top: 1px solid " (p/get-string-border-color) ";"
                                                       "border-radius:1px;"
                                                       "margin: 0px 0px 2px 0px;"
                                                       "background-color:" (p/get-string-background-color) ";")
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
   :meta-wrapper-style                            (str "border: 1px solid " (p/get-meta-color 0.4) ";"
                                                       "margin: -1px;"
                                                       "border-radius:2px;"
                                                       "display: inline-block;")
   :meta-style                                    (str "background-color:" (p/get-meta-color) ";"
                                                       "color:#eee;"
                                                       "border-radius: 0 1px 1px 0;"
                                                       "padding:0px 2px;"
                                                       "-webkit-user-select: none;")
   :meta-body-style                               (str "background-color: " (p/get-meta-color 0.1) ";"
                                                       "padding:1px;"
                                                       "padding-left: 14px;"
                                                       "border-bottom-right-radius:1x;")
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
   :bypass-availability-checks                    false


   ; markup tags
   :cljs-land-tag                                 [:span :cljs-land-style]
   :nil-tag                                       [:span :nil-style]
   :bool-tag                                      [:span :bool-style]
   :keyword-tag                                   [:span :keyword-style]
   :symbol-tag                                    [:span :symbol-style]
   :integer-tag                                   [:span :integer-style]
   :float-tag                                     [:span :float-style]
   :string-tag                                    [:span :string-style]
   :expanded-string-tag                           [:span :expanded-string-style]
   :circular-reference-tag                        [:span :circular-reference-wrapper-style]
   :native-reference-tag                          [:span :native-reference-wrapper-style]
   :meta-wrapper-tag                              [:span :meta-wrapper-style]
   :meta-header-tag                               [:span :meta-style]
   :meta-body-tag                                 [:span :meta-body-style]
   :meta-reference-tag                            [:span :meta-reference-style]
   :body-tag                                      [:span :body-style]
   :index-tag                                     [:span :index-style]
   :standard-ol-tag                               [:ol :standard-ol-style]
   :standard-ol-no-margin-tag                     [:ol :standard-ol-no-margin-style]
   :standard-li-tag                               [:li :standard-li-style]
   :standard-li-no-margin-tag                     [:li :standard-li-no-margin-style]
   :aligned-li-tag                                [:li :aligned-li-style]
   :body-items-more-tag                           [:span :body-items-more-style]
   :fn-args-tag                                   [:span :fn-args-style]
   :fn-name-tag                                   [:span :fn-name-style]
   :fn-prefix-tag                                 [:span :fn-prefix-style]
   :fn-header-tag                                 [:span :fn-header-style]
   :fn-multi-arity-args-indent-tag                [:span :fn-multi-arity-args-indent-style]
   :fn-ns-name-tag                                [:span :fn-ns-name-style]
   :type-wrapper-tag                              [:span :type-wrapper-style]
   :type-name-tag                                 [:span :type-name-style]
   :type-ref-tag                                  [:span :type-ref-style]
   :type-basis-tag                                [:span :type-basis-style]
   :type-basis-item-tag                           [:span :type-basis-item-style]
   :standalone-type-tag                           [:span :standalone-type-style]
   :header-field-tag                              [:span :header-field-style]
   :header-field-name-tag                         [:span :header-field-name-style]
   :header-field-value-tag                        [:span :header-field-value-style]
   :body-field-tr-tag                             [:tr :body-field-tr-style]
   :body-field-td1-tag                            [:td :body-field-td1-style]
   :body-field-td2-tag                            [:td :body-field-td2-style]
   :body-field-name-tag                           [:span :body-field-name-style]
   :body-field-value-tag                          [:span :body-field-value-style]
   :fields-header-tag                             [:span :fields-header-style]
   :protocol-method-arities-header-tag            [:span :protocol-method-arities-header-style]
   :protocol-method-tag                           [:span :protocol-method-style]
   :protocol-method-name-tag                      [:span :protocol-method-name-style]
   :protocol-ns-name-tag                          [:span :protocol-ns-name-style]
   :protocols-header-tag                          [:span :protocols-header-style]
   :instance-body-fields-table-tag                [:table :instance-body-fields-table-style]
   :instance-value-tag                            [:span :instance-value-style]
   :instance-custom-printing-wrapper-tag          [:span :instance-custom-printing-wrapper-style]
   :instance-header-tag                           [:span :instance-header-style]
   })

