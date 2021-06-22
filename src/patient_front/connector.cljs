(ns patient-front.connector
  (:require [reagent.core :as r :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [adzerk.env :as env]))

(env/def PATIENT_BACK_URL nil)

(def paths {:add           (str PATIENT_BACK_URL "/add-patient")
            :edit          (str PATIENT_BACK_URL "/patient/edit")
            :delete        (str PATIENT_BACK_URL "/patient/delete")
            :get-list      (str PATIENT_BACK_URL "/list")
            :get-by-id     (str PATIENT_BACK_URL "/patient")
            :get-addresses (str PATIENT_BACK_URL "/addresses")})

(defn send-post-request
  [type body]
  (go (let [response (<! (http/post (get paths type)
                           {:json-params body
                            :with-credentials? false}
                           ))]
        (prn (:status response))
        response
        )))

(defn send-get-request
  [type body]
  (go (let [response (<! (http/get (get paths type)
                           (merge
                             (when-not (nil? body) {:query-params body})
                             {:with-credentials? false})))]
        (prn (:status response))
        (:body response))))
