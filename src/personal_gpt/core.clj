(ns personal-gpt.core
  (:require
    [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [clojure.pprint :refer [pprint]]
            [wkok.openai-clojure.api :as api]
            )
  (:gen-class))

(def token (env :telegram-token))
(def gpt-token (env :gpt-token))

(defn exception-middleware
  [func]
  (fn [message]
    (try
      (func message)
      (catch Exception e
        (let [chat-id (or (-> message :callback_query :message :chat :id)
                          (-> message :message :chat :id))]
          (println "Произошло исключение\n" e "\n" "Для сообщения: ")
          (pprint message)
          (t/send-text token chat-id "Упс! Что-то пошло не так"))))))

(h/defhandler handler

  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (println "Bot joined new chat: " chat)
                  (t/send-text token id "Welcome to personal-gpt!")))

  (h/message-fn
    (fn [{:keys [text] {id :id} :chat :as message}]
      (if text
        (let [r (api/create-chat-completion
                  {:model    "gpt-3.5-turbo"
                   :messages [{:role "system" :content "You are a helpful assistant."}
                              {:role "user" :content text}]}
                  {:api-key gpt-token})
              r-text (-> r :choices first :message :content)]
          (t/send-text token id r-text))
        (t/send-text token id "you send nothing")))))

(comment
  (api/create-chat-completion
    {:model    "gpt-3.5-turbo"
     :messages [{:role "system" :content "You are a helpful assistant."}
                {:role "user" :content "привет как дела"}]}
    {:api-key gpt-token})
  )

(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the personal-gpt")
  (<!! (p/start token (exception-middleware handler)))
  )

(comment

  (def app (p/start token (exception-middleware handler)))
  (p/stop app)
  )
