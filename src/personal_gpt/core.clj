(ns personal-gpt.core
  (:require
    [clojure.core.async :refer [<!! <! timeout]]
    [clojure.string :as str]
    [environ.core :refer [env]]
    [morse.handlers :as h]
    [morse.polling :as p]
    [morse.api :as t]
    [clojure.pprint :refer [pprint]]
    [wkok.openai-clojure.api :as api]
    [personal-gpt.md-to-html :refer [md->html]])
  (:gen-class))

(def token (env :telegram-token))
(def gpt-token (env :gpt-token))

(defn exception-middleware
  [func]
  (fn [message]
    (try
      (func message)
      (catch Exception e
        (println "Произошло исключение\n" e "\n" "Для сообщения: ")
        (pprint message)
        (if-let [chat-id (or (-> message :callback_query :message :chat :id)
                             (-> message :message :chat :id)
                             (-> message :edited_message :from :id))]
          (try
              (t/send-text token chat-id "Упс! Что-то пошло не так")
            (catch Exception e
              (println "При отправке уведомления об ошибке пользователю произошла ошибка")
              (println e)))
          (println "chat_id не найден"))))))

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
          (t/send-text token id {:parse_mode "html"} (md->html r-text)))
        (t/send-text token id "you send nothing")))))

(comment
  (api/create-chat-completion
    {:model    "gpt-3.5-turbo"
     :messages [{:role "system" :content "You are a helpful assistant."}
                {:role "user" :content "привет как дела"}]}
    {:api-key gpt-token})
  )

(def run? (atom true))
(def current-chan (atom nil))

(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the personal-gpt")
  (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. ^Runnable (fn []
                           (println "Stopping gracefully")
                           (reset! run? false)
                           (try
                             (p/stop @current-chan)
                             (catch Exception e
                               (prn "Cant stop chan " e)))
                           (System/exit 0))))

  (loop [run @run?]
    (when run
      (let [ch (p/start token (exception-middleware handler))]
        (reset! current-chan ch)
        (let [res (<!! ch)]
          (prn "Channel closed. Sleeping for 5sec. Result: " res)
          (<!! (timeout 5000))
          (recur @run?)))))
  )

(comment

  (def app (p/start token (exception-middleware handler)))
  (p/stop app)
  )
