(ns devtools.defaults
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]))

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn make-color [r g b & [a]]
  (str "rgba(" r ", " g ", " b ", " (or a "1") ")"))

; -- colors -----------------------------------------------------------------------------------------------------------------

(defn make-signature-color [& [a]]
  (make-color 100 255 100 a))

(defn make-type-color [& [a]]
  (make-color 0 160 220 a))

(defn make-meta-color [& [a]]
  (make-color 255 102 0 a))

(defn make-protocol-color [& [a]]
  (make-color 65 105 225 a))

(defn make-method-color [& [a]]
  (make-color 65 105 225 a))

(defn make-ns-color [& [a]]
  (make-color 150 150 150 a))

(defn make-native-color [& [a]]
  (make-color 255 0 255 a))

(defn make-lambda-color [& [a]]
  (make-color 30 130 30 a))

(defn make-fn-color [& [a]]
  (make-color 30 130 30 a))

(defn make-string-color [& [a]]
  (make-color 255 100 100 a))

(defn make-custom-printing-color [& [a]]
  (make-color 255 255 200 a))

(defn make-circular-ref-color [& [a]]
  (make-color 255 0 0 a))

; -- color macros -----------------------------------------------------------------------------------------------------------

(defmacro get-signature-color [& [a]]
  (make-signature-color a))

(defmacro get-type-color [& [a]]
  (make-type-color a))

(defmacro get-meta-color [& [a]]
  (make-meta-color a))

(defmacro get-protocol-color [& [a]]
  (make-protocol-color a))

(defmacro get-method-color [& [a]]
  (make-method-color a))

(defmacro get-ns-color [& [a]]
  (make-ns-color a))

(defmacro get-native-color [& [a]]
  (make-native-color a))

(defmacro get-lambda-color [& [a]]
  (make-lambda-color a))

(defmacro get-fn-color [& [a]]
  (make-fn-color a))

(defmacro get-string-color [& [a]]
  (make-string-color a))

(defmacro get-custom-printing-color [& [a]]
  (make-custom-printing-color a))

(defmacro get-circular-ref-color [& [a]]
  (make-circular-ref-color a))

; -- specific color macros --------------------------------------------------------------------------------------------------

(defmacro get-signature-background-color []
  (get-signature-color 0.08))

(defmacro get-body-border-color []
  (get-signature-color 0.4))

(defmacro get-string-background-color []
  (get-string-color 0.08))

(defmacro get-string-border-color []
  (get-string-color 0.4))

(defmacro get-custom-printing-background-color []
  (get-custom-printing-color 0.4))

; -- styling helpers --------------------------------------------------------------------------------------------------------

(defn eval-css-arg [arg-form]
  (if (sequential? arg-form)
    (let [form `(do
                  (alias '~'d '~'devtools.defaults)                                                                           ; this trick introduces proper alias to p symbol used in defaults.cljs
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
  `(css "color: #eef;"
        "padding: 0px 2px 0px 2px;"
        "-webkit-user-select: none;"))

(defmacro get-inner-background-style []
  `(css "position: absolute;"
        "top: 1px;"
        "right: 1px;"
        "bottom: 1px;"
        "left: 1px;"
        "border-radius: 1px;"))

(defmacro get-custom-printing-background-style []
  `(css (str "background-color:" (get-custom-printing-background-color) ";")
        (get-inner-background-style)
        (str "border-left: 1px solid " (get-type-color 0.5) ";")
        "border-radius: 0 1px 1px 0;"))

(defmacro get-instance-type-header-background-style []
  `(css (str "background-color:" (get-type-color 0.5) ";")
        (get-inner-background-style)))

(defmacro get-protocol-background-style []
  `(css (str "background-color:" (get-protocol-color 0.5) ";")
        (get-inner-background-style)))

(defmacro get-native-reference-background-style []
  `(css "position: absolute;"
        "top: 3px;"
        "right: 1px;"
        "bottom: 1px;"
        "left: 1px;"
        "border-radius: 1px;"
        "background-color: white;"))

(defmacro get-common-protocol-style []
  `(css "position: relative;"
        "padding: 0px 4px;"
        "border-radius: 2px;"
        "-webkit-user-select: none;"))

; -- style macros -----------------------------------------------------------------------------------------------------------

(defmacro make-style [style]
  `(cljs.core/js-obj "style" ~style))

(defmacro symbol-style [color & [kind]]
  `(css (str "background-color:" ~color ";")
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
        "-webkit-user-select: none;"
        (if (= ~kind :slim)
          "padding: 0px 4px; top:2px;"
          "padding: 1px 4px; top:1px;")))

(defmacro icon [label & [color slim?]]
  `[[:span (symbol-style (or ~color "#000") ~slim?)] ~label])

(defmacro type-outline-style []
  `(css (str "box-shadow:0px 0px 0px 1px " (get-type-color 0.5) " inset;")
        "border-radius: 2px;"))

; -- markup helpers ---------------------------------------------------------------------------------------------------------

(defmacro span-markup [style & content]
  `[[:span ~style] ~@content])

(defmacro get-instance-type-header-background-markup []
  `(span-markup (get-instance-type-header-background-style)))

(defmacro get-protocol-background-markup []
  `(span-markup (get-protocol-background-style)))

(defmacro get-native-reference-background-markup []
  `(span-markup (get-native-reference-background-style)))

(defmacro get-custom-printing-background-markup []
  `(span-markup (get-custom-printing-background-style)))
