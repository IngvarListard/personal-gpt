(ns personal-gpt.md-to-html
  (:require [clojure.string]
            [markdown.common
             :refer
             [bold
              bold-italic
              dashes
              em
              escape-inhibit-separator
              escaped-chars
              heading-text
              inhibit
              inline-code
              italics
              strikethrough
              strong
              thaw-strings]]
            [markdown.core :as md]
            [markdown.links
             :refer [image
                     image-reference-link
                     implicit-reference-link
                     link
                     ]]
            [markdown.tables :refer [table]]
            [markdown.transformers :as ts]))

(defn heading [text {:keys [buf next-line code codeblock heading-anchors] :as state}]
  (cond
    (or codeblock code)
    [text state]

    (ts/h1? (or buf next-line))
    [(str "<b>" text "</b>" "\n") state]

    (ts/h2? (or buf next-line))
    [(str "<b>" text "</b>" "\n") state]

    :else
    (if-let [heading-text* (heading-text text)]
      [(str heading-text* "\n") state]
      [text state])))

(defn escape-tg-markdown
  "Change special characters into HTML character entities."
  [text state]
  [(if-not (or (:code state) (:codeblock state))
     (clojure.string/escape
       text
       {\& "&amp;"
        \< "&lt;"
        \> "&gt;"
        \" "&quot;"})
     text) state])

(def transformers
  [escape-tg-markdown
   ts/set-line-state
   ts/empty-line
   inhibit
   escape-inhibit-separator
   ts/code
   ts/codeblock
   escaped-chars
   inline-code
   ts/autoemail-transformer
   ts/autourl-transformer
   image
   image-reference-link
   link
   implicit-reference-link
   ts/hr
   ts/blockquote-1
   heading
   ts/blockquote-2
   italics
   bold-italic
   em
   strong
   bold
   strikethrough
   ts/superscript
   table
   ts/br
   thaw-strings
   dashes
   ts/clear-line-state])

(defn md->html
  [text]
  (md/md-to-html-string text :replacement-transformers transformers))
