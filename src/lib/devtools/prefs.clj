(ns devtools.prefs)

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

(defmacro get-body-line-common-style []
  `(str "min-height: 14px;"))

(defmacro get-common-type-header-style []
  `(str "color: #eef;"
        "padding: 0px 2px 0px 2px;"
        "-webkit-user-select: none;"))

(defmacro get-inner-background []
  `(str "position: absolute;"
        "top: 1px;"
        "right: 1px;"
        "bottom: 1px;"
        "left: 1px;"
        "border-radius: 1px;"))

(defmacro get-custom-printing-background []
  `(str "background-color:" (get-custom-printing-background-color) ";"
        (get-inner-background)
        "border-left: 1px solid " (get-type-color 0.5) ";"
        "border-radius: 0 1px 1px 0;"))

(defmacro get-instance-type-header-background []
  `(str "background-color:" (get-type-color 0.5) ";"
        (get-inner-background)))

(defmacro get-protocol-background []
  `(str "background-color:" (get-protocol-color 0.5) ";"
        (get-inner-background)))

(defmacro get-native-reference-background []
  `(str "position: absolute;"
        "top: 3px;"
        "right: 1px;"
        "bottom: 1px;"
        "left: 1px;"
        "border-radius: 1px;"
        "background-color: white;"))

(defmacro get-common-protocol-style []
  `(str "position: relative;"
        "padding: 0px 4px;"
        "border-radius: 2px;"
        "-webkit-user-select: none;"))

; -- style macros -----------------------------------------------------------------------------------------------------------

(defmacro symbol-style [color & [slim?]]
  `(str "background-color:" ~color ";"
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
        (if ~slim?
          "padding: 0px 4px; top:2px;"
          "padding: 1px 4px; top:1px;")))

(defmacro icon [label & [color slim?]]
  `(cljs.core/array "span" (cljs.core/js-obj "style" (symbol-style (or ~color "#000") ~slim?)) ~label))

