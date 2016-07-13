(ns devtools.prefs)

(defmacro symbol-style [background-color]
  (str "background-color:" background-color ";"
       "color: #fff;"
       "width: 20px;"
       "display: inline-block;"
       "text-align: center;"
       "font-size: 8px;"
       "opacity: 0.5;"
       "position: relative;"
       "top: -1px;"
       "margin-right: 3px;"
       "padding: 1px 4px 1px 4px;"
       "border-radius: 2px;"
       "-webkit-user-select: none;"))
