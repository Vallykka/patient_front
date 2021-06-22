(ns ^:figwheel-hooks patient-front.core
  (:require
    [goog.dom :as gdom]
    [reagent.dom :as rdom]
    [patient-front.view :as v]))

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount [el]
  (v/get-patients 0 @v/limit nil)
  (rdom/render [v/app] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element)
  )
