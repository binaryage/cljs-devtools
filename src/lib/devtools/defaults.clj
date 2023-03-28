(ns devtools.defaults
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]))

(declare make-named-color)
(declare make-color)

(def named-colors
  {:base-text-color             [0 0 0]                                                                                       ; black text color, used to prevent leaking in text colors from "outside"
   :signature                   [100 255 100]
   :type                        [0 160 220]
   :type-text                   [238 238 355]
   :field                       #(make-named-color :type)
   :basis                       #(make-named-color :type)
   :meta                        [255 102 0]
   :meta-text                   [238 238 238]
   :protocol                    [41 59 163]
   :method                      #(make-named-color :protocol)
   :ns                          [150 150 150]
   :native                      [255 0 255]
   :fn                          [30 130 30]
   :lambda                      #(make-named-color :fn)
   :fn-args                     [170 130 20]
   :custom-printing             [255 255 200]
   :circular-ref                [255 0 0]
   :nil                         [128 128 128]
   :keyword                     [136 19 145]
   :integer                     [28 0 207]
   :float                       [28 136 207]
   :float-nan                   [213 60 27]
   :float-infinity              [28 80 207]
   :string                      [196 26 22]
   :expanded-string             [255 100 100]
   :symbol                      [0 0 0]
   :bool                        [0 153 153]
   :fast-protocol               [255 255 170]
   :slow-protocol               [238 238 238]
   :more                        [255 255 255]
   :more-background             [153 153 153]
   :index                       [0 0 0]
   :index-background            [221 221 221]
   :field-spacer                [204 204 204]
   :native-reference-background [255 255 255]
   :signature-background        #(make-named-color :signature 0.08)
   :body-border                 #(make-color 60 90 60 0.1)
   :expanded-string-background  #(make-named-color :expanded-string 0.08)
   :expanded-string-border      #(make-named-color :expanded-string 0.4)
   :custom-printing-background  #(make-named-color :custom-printing)})

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn make-color [r g b & [a]]
  {:pre [(number? r)
         (number? g)
         (number? b)
         (or (nil? a) (number? a))]}
  (str "rgba(" r ", " g ", " b ", " (or a "1") ")"))

(defn resolve-color [v]
  (if (fn? v)
    (recur (v))
    v))

(defn make-named-color [name & [a]]
  (if-let [res (resolve-color (name named-colors))]
    (cond
      (string? res) res
      (vector? res) (let [[r g b] res]
                      (make-color r g b a))
      :else (assert false (str "invalid result from named-colors lookup table: " res)))
    (assert false (str "unable to lookup named color: " name "\n"
                       "avail names:" (keys named-colors)))))

; -- color macros -----------------------------------------------------------------------------------------------------------

(defmacro named-color [name & [a]]
  (make-named-color name a))

; -- styling helpers --------------------------------------------------------------------------------------------------------

(defn eval-css-arg [arg-form]
  (if (sequential? arg-form)
    (let [form `(do
                  ; warning: keep this in sync with defaults.cljs!
                  (require '~'[devtools.defaults :as d :refer [css span named-color]])
                  ~arg-form)]
      (binding [*ns* (find-ns 'clojure.core)]
        (eval form)))
    arg-form))

(defn sanitize-css [css-string]
  (-> css-string
      (string/replace #"([:,;])\s+" "$1")
      (string/trim)))

(defn ^:dynamic check-css-semicolon [css-part input-css]
  (assert (re-matches #".*;$" css-part) (str "stitched css expected to end with a semicolon: '" (pr-str css-part) "'\n"
                                             "input css form:" (with-out-str (pprint input-css))))
  css-part)

(defn check-semicolons [v]
  (doseq [item v]
    (check-css-semicolon item v))
  v)

(defmacro css
  "This magical macro evals all args in the context of this namespace. And concatenates resulting strings.
  The goal is to emit one sanitized css string to be included in cljs sources.
  This macro additionally checks for missing semicolons. Each arg must end with a semicolon."
  [& args]
  (if-not (empty? args)
    (let [evald-args (map eval-css-arg args)]
      (assert (every? string? evald-args)
              (str "all css args expected to be eval'd to strings or vectors of strings:\n"
                   (with-out-str (pprint evald-args))))
      (sanitize-css (string/join (check-semicolons evald-args))))))

(defmacro get-body-line-common-style []
  `(css "min-height: 14px;"))

(defmacro get-common-type-header-style []
  `(css (str "color: " (named-color :type-text) ";")
        "padding: 0px 2px 0px 2px;"
        "user-select: none;"))

(defmacro get-inner-background-style []
  `(css "position: absolute;"
        "top: 1px;"
        "right: 1px;"
        "bottom: 1px;"
        "left: 1px;"
        "border-radius: 1px;"))

(defmacro get-custom-printing-background-style []
  `(css (str "background-color:" (named-color :custom-printing-background) ";")
        (get-inner-background-style)
        "border-radius: 0 1px 1px 0;"))

(defmacro get-instance-type-header-background-style []
  `(css (str "background-color:" (named-color :type 0.5) ";")
        (get-inner-background-style)))

(defmacro get-protocol-background-style []
  `(css (str "background-color:" (named-color :protocol 0.5) ";")
        (get-inner-background-style)))

(defmacro get-native-reference-background-style []
  `(css (str "background-color:" (named-color :native-reference-background) ";")
        (get-inner-background-style)))

(defmacro get-common-protocol-style []
  `(css "position: relative;"
        "padding: 0px 4px;"
        "border-radius: 2px;"
        "user-select: none;"))

; -- style macros -----------------------------------------------------------------------------------------------------------

(defmacro make-style [style]
  `(cljs.core/js-obj "style" ~style))

(defmacro symbol-style [color & [kind]]
  `(css (str "background-color: " ~color ";")
        "color: #fff;"
        "width: 20px;"
        "display: inline-block;"
        "text-align: center;"
        "font-size: 8px;"
        "opacity: 0.5;"
        "vertical-align: top;"
        "position: relative;"
        "margin-right: 3px;"
        "border-radius: 2px;"
        "user-select: none;"
        (if (= ~kind :slim)
          "padding: 0px 4px; top:2px;"
          "padding: 1px 4px; top:1px;")))

(defmacro icon [label & [color slim?]]
  `[[:span (symbol-style (or ~color "#000") ~slim?)] ~label])

(defmacro type-outline-style []
  `(css (str "box-shadow: 0px 0px 0px 1px " (named-color :type 0.5) " inset;")
        "margin-top: 1px;"
        "border-radius: 2px;"))

; -- markup helpers ---------------------------------------------------------------------------------------------------------

(defmacro markup [tag style & content]
  `[[~tag ~style] ~@content])

(defmacro span [style & content]
  `(markup :span ~style ~@content))

(defmacro get-instance-type-header-background-markup []
  `(span (get-instance-type-header-background-style)))

(defmacro get-protocol-background-markup []
  `(span (get-protocol-background-style)))

(defmacro get-native-reference-background-markup []
  `(span (get-native-reference-background-style)))

(defmacro get-custom-printing-background-markup []
  `(span (get-custom-printing-background-style)))
