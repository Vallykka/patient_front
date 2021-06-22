(ns patient-front.view
  (:require  [reagent.core :as r :refer [atom]]
             [patient-front.connector :as c]
             [patient-front.validator :as v]
             [cljs-time.core :as t]
             [cljs-time.format :as tf]
             [cljs.core.async :refer [<!]]
             [goog.string :as gs]
             [goog.string.format]
             [clojure.string :refer [blank? join]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce patient-model (r/atom {:id         {:label "id" :invisible true :value "" :required false}
                                :surname    {:label "Фамилия" :value ""  :required true}
                                :name       {:label "Имя" :value "" :required true}
                                :patronymic {:label "Отчество" :value "" :required false}
                                :sex        {:label "Пол" :value "" :display-value {"MALE" "Мужчина" "FEMALE" "Женщина"} :required true}
                                :birth-date {:label "Дата рождения" :value "" :required true}
                                :address    {:label "Адрес" :value "" :required false
                                             :address-id ""
                                             :country {:label "Страна" :value "" :required true}
                                             :region {:label "Регион" :value "" :required true}
                                             :city {:label "Город" :value "" :required true}
                                             :street {:label "Улица" :value "" :required true}
                                             :house {:label "Дом" :value "" :required true}
                                             :flat {:label "Квартира" :value "" :required false}}
                                :oms-policy {:label "Номер полиса ОМС" :value "" :required true}}) )

(defonce messages (r/atom {:add-error "Не удалось добавить пациента: %s"
                           :edit-error "Не удалось сохранить пациента: %s"
                           :delete-error "Не удалось удалить пациента: %s"
                           :validation-error  "Отсутствует обязательное поле: %s"
                           :add-success "Успешно сохранено"
                           :edit-success "Успешно обновлено"
                           :delete-success "Успешно удалено"}))

(defonce patients (r/atom {:count 0
                           :patients ()}))
(defonce limit (r/atom 10))

(defonce displayed-errors (r/atom {:message ""}))
(defonce displayed-info (r/atom {:message ""}))

(defn reformat-date
  [date]
  (when (not (blank? date))
   (tf/unparse (tf/formatter "dd-MM-yyyy") (tf/parse (tf/formatters :date) date))))

(defn dto
  [model]
  (let [set-values (apply merge {} (map (fn [e] {(key e) (:value (val e))}) model))
        filtered  (filter (fn [e] (not= (key e) :label)) (:address model))
        addr (apply merge {} (map (fn [v] {(first v) (second v)}) filtered))
        res (assoc set-values :address addr)
        date (reformat-date (:birth-date res))]
    (assoc res :birth-date date)
    ))

(defn format-address
  [address]
  (merge
    (select-keys (:data address) [:country :region :city :street :house :flat])
    (select-keys address [:value :address-id])))

(defn format-date
  [date]
  (if-not (blank? date)
    (tf/unparse (tf/formatters :date) (tf/parse (tf/formatter "dd-MM-yyyy") date))
    ""))

(defn prepare-message
  [type fields]
  (gs/format (get @messages type) (join ", "  fields)))

(defn get-patients
  [offset limit params]
  (go (let [off-lim {:offset offset :limit limit}
            req (if-not (nil? params)
                  (merge {} params off-lim)
                  off-lim)
            resp (<! (c/send-get-request :get-list
                       (if-not (nil? params)
                         (merge {} params off-lim)
                         off-lim)))]
        (pr params req)
        (reset! patients resp)
        )))

(defn clear-patient-model
  []
  (doall
    (map (fn [[k props]] (swap! patient-model assoc-in [k :value] ""))
      @patient-model))
  (doall
   (map (fn [[k v]] (swap! patient-model assoc-in [:address k :value] ""))
     (select-keys (:address @patient-model) [:country :region :city :street :house :flat])))
  (swap! patient-model assoc-in [:address :address-id] "")
  (swap! patient-model assoc-in [:address :value] ""))

(defn refresh-table
  []
  (get-patients 0 @limit nil))

(defn show-messages
  [error info]
  [:div {:id :notes}
   [:div {:id :exception
          :class (if (blank? (:message error)) :hidden :visible)
          :on-click #(reset! displayed-errors {:message ""})}
    [:span {:id :exception}
     (:message error)]
    ]
   [:div {:id :info
          :class (if (blank? (:message info)) :hidden :visible)
          :on-click #(reset! displayed-info {:message ""})}
    [:span {:id :info}
     (:message info)]
    ]
  ])

(defonce address-value (r/atom ()))
(defonce visible-search (r/atom false))
(defn add-address
  [patient]
  [:div {:id :address}
   [:input {:id          :address
            :class (if @visible-search :hidden :visible)
            :placeholder "Страна, регион, город, улица, дом"
            :value   (get-in patient [:address :value])
            :read-only  true
            :on-focus    #(reset! visible-search true)
            }]
   [:input {:class       (if-not @visible-search :hidden :visible)
            :type        "text"
            :id          :search-addr
            :placeholder "Страна, регион, город, улица, дом"
            :on-change   (fn [e]
                           (let [val (.. e -target -value)]
                             (go (let [resp (<! (c/send-post-request :get-addresses {:query val}))]
                                   (reset! address-value (get-in resp [:body :suggestions]))))
                             ))
            }]
   [:ul {:id :addresses}
    (map (fn [addr] ^{:key (:value addr)}
           [:li {:on-click (fn [e]
                             (swap! patient-model assoc-in [:address] (format-address addr))
                             (reset! visible-search false)
                             (reset! address-value ()))}
            (:value addr)]) @address-value)]
   ])

(defn add-input
  [patient]
  [:div {:id :inputs}
   (map
     (fn [[key props]] ^{:key (str "add-" key)}
       [:input {:id          (str "add-" key)
                :placeholder (:label props)
                :value       (:value props)
                :on-change   #(when (:value props) (swap! patient-model assoc-in [key :value] (.. % -target -value)))
                }])
     (seq (filter (fn [[key props]] (and (not (:invisible props)) (not-any? #(= key %) (list :sex :birth-date :address))))
         patient)
       ))
   [:div {:id :sex}
     (map (fn [[k v]]
            ^{:key (str "div-radio-" k)}
            [:div {:id (str "div-radio-" k)} ^{:key (str "radio-" k)}
             [:input {:id (str "radio-" k)
                      :checked (= (get-in patient [:sex :value]) k)
                      :type :radio
                      :name :sex
                      :value k
                      :on-change #(swap! patient-model assoc-in [:sex :value] (.. % -target -value))}]
             [:label {:for k} v]]
            ) (get-in patient [:sex :display-value]))
     ]
   [:div {:id :date}
    [:label {:for :birth-date} (get-in patient [:birth-date :label])]
    ^{:key :date}
    [:input {:type        :date
             :value       (get-in patient [:birth-date :value] "")
             :on-change   #(swap! patient-model assoc-in [:birth-date :value] (.. % -target -value))
             }]]
   (add-address patient)
   ])

(defn add-form
  [patient]
  [:div {:id :patient-form}
   (add-input patient)
   [:button {:id       :save
             :on-click (fn [e]
                         (if-let [exc (not-empty (v/invalid? patient))]
                           (swap! displayed-errors assoc :message (prepare-message :validation-error exc))
                           (go (let [add? (blank? (get-in patient [:id :value]))
                                     resp (<! (c/send-post-request (if add? :add :edit) (dto patient)))]
                                 (if (= (:status resp) 200)
                                   (do
                                     (swap! displayed-errors assoc :message "")
                                     (swap! displayed-info assoc :message (prepare-message (if add? :add-success :edit-success) ""))
                                     (clear-patient-model)
                                     (refresh-table))
                                   (swap! displayed-errors assoc :message (prepare-message (if add? :add-error :edit-error) ""))
                                   )
                                 ))))} "Сохранить"]
   [:button {:id       :delete
             :on-click (fn [e]
                         (go (let [resp (<! (c/send-post-request :delete
                                              {:id (get-in patient [:id :value])}))]
                               (if (= (:status resp) 200)
                                 (do (swap! displayed-errors assoc :message "")
                                     (swap! displayed-info assoc :message (prepare-message :delete-success ""))
                                     (clear-patient-model)
                                     (refresh-table)
                                     )
                                 (swap! displayed-errors assoc :message (prepare-message :delete-error ""))
                                 )
                               )))} "Удалить"]
   [:button {:id :clean
             :on-click (fn [e] (clear-patient-model))
             } "Очистить"]
   ])


(defonce filters (r/atom {}))
(defn add-filters
  [patient]
  [:div {:id :filters}
   (map
     (fn [[key props]] ^{:key (str "filter-" key)}
       [:input {:id (str "filter-" key)
                :placeholder (:label props)
                :on-change   #(swap! filters assoc key (.. % -target -value))
                }])
     (seq (filter (fn [[key props]] (and (not (:invisible props)) (not-any? #(= key %) (list :sex :birth-date :address))))
            patient)))
   [:button {:id       :filter
             :on-click (fn [] (get-patients 0 @limit (into {} (remove #(apply (every-pred nil? blank?) (val %)) @filters))))
             } "Отфильтровать"]
   ])

(defn add-columns
  [patient]
  (map
    (fn [[column-id props]] ^{:key (str "column-" column-id)}
      [:th
       {:id    column-id
        :class (when (:invisible props) "hidden")}
       (:label props)]) patient))

(defn add-rows
  [patients]
  (doall (map (fn [patient] ^{:key (str "row-" (:id patient))}
          [:tr {:id       :patient
                :on-click (fn [e] (go (let [resp (<! (c/send-post-request :get-by-id {:id (:id patient)}))
                                            patient-to-upd (:body resp)]
                                        (doall
                                          (map (fn [[key props]]
                                                 (swap! patient-model assoc-in [key :value] (get patient-to-upd key ""))) @patient-model))
                                        (doall (let [addr (select-keys patient-to-upd [:country :region :city :street :house :flat])]
                                                 (map (fn [[key v]] (swap! patient-model assoc-in [:address key :value] v)) addr)))
                                        (swap! patient-model assoc-in [:address :address-id] (str (get patient-to-upd :address-id)))
                                        (swap! patient-model assoc-in [:birth-date :value] (format-date (get patient-to-upd :birth-date)))
                                        )
                                    ))}
           (doall (map
              (fn [[key props]] ^{:key (str "row-" (:id patient) key)}
                [:td {:id    key
                      :class (when (:invisible props) :hidden)}
                 (if-let [displayed (:display-value props)]
                   (get displayed (get patient key))
                   (get patient key))])
              @patient-model))]
          )
     (:patients patients))))

(defn add-pagination
  [count]
  (let [last (if (zero? (rem count @limit)) (quot count @limit) (inc (quot count @limit)))]
   [:div {:id :pagination}
    [:button {:id       :first
              :on-click (fn [e] (get-patients 0 @limit nil))} "<"]
    (map (fn [p] ^{:key p}
        [:button {:id       :page
                  :on-click (fn [e] (get-patients (* (dec p) @limit) @limit nil))} p])
      (range 1 (inc last)))
    [:button {:id       :last
              :on-click (fn [e] (get-patients (* (dec last) @limit) @limit nil))} ">"]
    ]))

(defn add-table
  [patients]
  [:div {:id :patients}
   [:span {:id :patients :class :table-title} "Пациенты"]
   [:table {}
    [:thead {}
     ^{:key (str "patients-table")}
     [:tr {}
      (add-columns @patient-model)
      ]]
    [:tbody {}
     (add-rows patients)
     ]
    ]
   (add-pagination (:count patients))
   ])

(defn app []
  [:div
   (show-messages @displayed-errors @displayed-info)
   (add-form @patient-model)
   (add-filters @patient-model)
   (add-table @patients)
   ])

