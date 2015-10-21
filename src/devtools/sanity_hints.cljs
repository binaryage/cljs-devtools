(ns devtools.sanity-hints
  (:require [devtools.prefs :refer [pref]]
            [cljs.stacktrace :as stacktrace]
            [clojure.string :as str]))

; Question: How much time have you lost staring at "Cannot read property 'call' of null" kind of errors?
;
; -------------------------------------------------------------------------------------------------------------------
;
; The idea is to try enhance error object's .stack and .message fields with additional info about
; the call site causing null type error. With optimizations :none the name of the null call site can be seen.
;
; The enahncing handler function tries to:
; 1) parse error's stack trace.
; 2) look original javascript source file up (via sync AJAX fetch by default).
; 3) locate reported line and column.
; 4) presents problematic line with a column hint as addition to .stack or .message strings.

; Technically the trick here is to override TypeError.prototype.toString
; and global window.onerror handler to enhance uncaught errors.
;
; With that we should handle two situations:
; 1) either error gets printed (typically in user's catch via console), so patched toString() method gets called.
; 2) or it is uncaught and our global error handler should take care of possible enhancement
;    before devtools present it to the user themselves.
;
; note: Tested under Chrome only

(defonce ^:dynamic *original-global-error-handler* nil)
(defonce ^:dynamic *original-type-error-prototype-to-string* nil)

(defonce *processed-errors* (if (exists? js/WeakSet) (js/WeakSet.)))                                                  ; note: phantomjs does not have WeakSet yet

(defn empty-as-nil [str]
  (if (empty? str) nil str))

(defn ajax-reader [url]
  (let [xhr (js/XMLHttpRequest.)]
    (.open xhr "GET" url false)
    (.send xhr)
    (empty-as-nil (.-responseText xhr))))

(defn retrieve-javascript-source [where]
  (let [reader (or (pref :file-reader) ajax-reader)]
    (reader where)))

(defn get-line [lines line-number]
  (aget lines (dec line-number)))                                                                                     ; line numbering is 1-based

(defn extend-content [content lines line-number min-length]
  (if (or (> (count content) min-length)
        (not (pos? line-number)))
    content
    (let [prev-line-number (dec line-number)
          prev-line (get-line lines prev-line-number)
          new-content (str prev-line "\n" content)]
      (extend-content new-content lines prev-line-number min-length))))

(defn mark-call-closed-at-column [line column]
  (let [n (dec column)                                                                                                ; column number is 1-based
        prefix (.substring line 0 n)
        postfix (.substring line n)]
    (str prefix " <<< ☢ RETURNED NULL ☢ <<< " postfix)))

(defn mark-null-call-site-location [file line-number column]
  (let [content (retrieve-javascript-source file)
        lines (.split content "\n")
        line (get-line lines line-number)
        marked-line (mark-call-closed-at-column line column)
        min-length (or (pref :sanity-hint-min-length) 128)]
    (extend-content marked-line lines line-number min-length)))

(defn make-sense-of-the-error [message file line-number column]
  (cond
    (re-matches #"Cannot read property 'call' of.*" message) (mark-null-call-site-location file line-number column)
    :else nil))

(defn error-object-sense [error]
  (let [native-stack-trace (.-stack error)
        stack-trace (stacktrace/parse-stacktrace {} native-stack-trace {:ua-product :chrome} {:asset-root ""})
        top-item (second stack-trace)                                                                                 ; first line is just an error message
        {:keys [file line column]} top-item]
    (make-sense-of-the-error (.-message error) file line column)))

(defn type-error-to-string []
  (this-as self
    (if *processed-errors*
      (when-not (.has *processed-errors* self)
        (.add *processed-errors* self)
        (when-let [sense (error-object-sense self)]
          (set! (.-message self) (str (.-message self) ", a sanity hint:\n" sense)))))                                ; this is dirty, patch message field before it gets used
    (.call *original-type-error-prototype-to-string* self)))

(defn global-error-handler [message url line column error]
  (let [res (if *original-global-error-handler*
              (*original-global-error-handler* message url line column error))]
    (if-not res
      (when-let [sense (error-object-sense error)]
        (.info js/console "A sanity hint for incoming uncaught error:\n" sense)
        false)
      true)))

(defn install-type-error-enhancer []
  (set! *original-global-error-handler* (.-onerror js/window))
  (set! (.-onerror js/window) global-error-handler)
  (let [prototype (.-prototype js/TypeError)]
    (set! *original-type-error-prototype-to-string* (.-toString prototype))
    (set! (.-toString prototype) type-error-to-string)))

(defn install! []
  (install-type-error-enhancer))

(defn uninstall! []
  (assert *original-type-error-prototype-to-string*)
  (set! (.-onerror js/window) *original-global-error-handler*)
  (let [prototype (.-prototype js/TypeError)]
    (set! (.-toString prototype) *original-type-error-prototype-to-string*)))
