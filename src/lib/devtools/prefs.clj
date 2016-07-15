(ns devtools.prefs)

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn make-color [r g b & [a]]
  (str "rgba(" r ", " g ", " b ", " (or a "1") ")"))

; -- colors -----------------------------------------------------------------------------------------------------------------

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

; -- color macros -----------------------------------------------------------------------------------------------------------

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

