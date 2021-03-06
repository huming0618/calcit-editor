
(ns app.updater.writer
  (:require [app.util :refer [bookmark->path to-writer to-bookmark push-info cirru->tree]]
            [app.util.stack :refer [push-bookmark]]
            [app.util.list :refer [dissoc-idx]]
            [app.schema :as schema]))

(defn collapse [db op-data session-id op-id op-time]
  (-> db
      (update-in
       [:sessions session-id :writer]
       (fn [writer]
         (-> writer (update :stack (fn [stack] (subvec stack op-data))) (assoc :pointer 0))))))

(defn draft-ns [db op-data sid op-id op-time]
  (-> db (update-in [:sessions sid :writer] (fn [writer] (assoc writer :draft-ns op-data)))))

(defn edit [db op-data session-id op-id op-time]
  (let [ns-text (get-in db [:sessions session-id :writer :selected-ns])
        bookmark (assoc op-data :ns ns-text :focus [])]
    (-> db
        (update-in [:sessions session-id :writer] (push-bookmark bookmark))
        (assoc-in [:sessions session-id :router] {:name :editor}))))

(defn edit-ns [db op-data sid op-id op-time]
  (let [writer (to-writer db sid), bookmark (to-bookmark writer), ns-text (:ns bookmark)]
    (if (contains? #{:def :proc} (:kind bookmark))
      (-> db
          (update-in
           [:sessions sid :writer]
           (push-bookmark (assoc schema/bookmark :kind :ns :ns ns-text))))
      db)))

(defn finish [db op-data sid op-id op-time]
  (-> db
      (update-in
       [:sessions sid :writer]
       (fn [writer]
         (let [pointer (:pointer writer)]
           (-> writer
               (update
                :stack
                (fn [stack] (if (> (count stack) pointer) (dissoc-idx stack pointer) stack)))
               (assoc :pointer (if (pos? pointer) (dec pointer) pointer))))))))

(defn focus [db op-data session-id op-id op-time]
  (let [writer (get-in db [:sessions session-id :writer])]
    (assoc-in db [:sessions session-id :writer :stack (:pointer writer) :focus] op-data)))

(defn go-down [db op-data session-id op-id op-time]
  (let [writer (get-in db [:sessions session-id :writer])
        bookmark (get (:stack writer) (:pointer writer))
        target-expr (get-in db (bookmark->path bookmark))]
    (if (zero? (count (:data target-expr)))
      db
      (-> db
          (update-in
           [:sessions session-id :writer :stack (:pointer writer) :focus]
           (fn [focus] (conj focus (apply min (keys (:data target-expr))))))))))

(defn go-left [db op-data session-id op-id op-time]
  (let [writer (get-in db [:sessions session-id :writer])
        bookmark (get (:stack writer) (:pointer writer))
        parent-bookmark (update bookmark :focus butlast)
        parent-path (bookmark->path parent-bookmark)
        last-coord (last (:focus bookmark))
        base-expr (get-in db parent-path)
        child-keys (vec (sort (keys (:data base-expr))))
        idx (.indexOf child-keys last-coord)]
    (if (empty? (:focus bookmark))
      db
      (-> db
          (update-in
           [:sessions session-id :writer :stack (:pointer writer) :focus]
           (fn [focus]
             (conj
              (vec (butlast focus))
              (if (zero? idx) last-coord (get child-keys (dec idx))))))))))

(defn go-right [db op-data session-id op-id op-time]
  (let [writer (get-in db [:sessions session-id :writer])
        bookmark (get (:stack writer) (:pointer writer))
        parent-bookmark (update bookmark :focus butlast)
        parent-path (bookmark->path parent-bookmark)
        last-coord (last (:focus bookmark))
        base-expr (get-in db parent-path)
        child-keys (vec (sort (keys (:data base-expr))))
        idx (.indexOf child-keys last-coord)]
    (if (empty? (:focus bookmark))
      db
      (-> db
          (update-in
           [:sessions session-id :writer :stack (:pointer writer) :focus]
           (fn [focus]
             (conj
              (vec (butlast focus))
              (if (= idx (dec (count child-keys))) last-coord (get child-keys (inc idx))))))))))

(defn go-up [db op-data session-id op-id op-time]
  (-> db
      (update-in
       [:sessions session-id :writer]
       (fn [writer]
         (update-in
          writer
          [:stack (:pointer writer) :focus]
          (fn [focus] (if (empty? focus) focus (vec (butlast focus)))))))))

(defn hide-peek [db op-data sid op-id op-time]
  (assoc-in db [:sessions sid :writer :peek-def] nil))

(defn move-next [db op-data sid op-id op-time]
  (-> db
      (update-in
       [:sessions sid :writer]
       (fn [writer]
         (let [pointer (:pointer writer)]
           (assoc
            writer
            :pointer
            (if (>= pointer (dec (count (:stack writer)))) pointer (inc pointer))))))))

(defn move-previous [db op-data sid op-id op-time]
  (-> db
      (update-in
       [:sessions sid :writer]
       (fn [writer]
         (let [pointer (:pointer writer)]
           (assoc writer :pointer (if (pos? pointer) (dec pointer) 0)))))))

(defn paste [db op-data sid op-id op-time]
  (let [writer (to-writer db sid)
        bookmark (to-bookmark writer)
        data-path (bookmark->path bookmark)
        user-id (get-in db [:sessions sid :user-id])]
    (if (vector? op-data)
      (-> db (assoc-in data-path (cirru->tree op-data user-id op-time)))
      db)))

(defn point-to [db op-data session-id op-id op-time]
  (assoc-in db [:sessions session-id :writer :pointer] op-data))

(defn remove-idx [db op-data session-id op-id op-time]
  (-> db
      (update-in
       [:sessions session-id :writer]
       (fn [writer]
         (-> writer
             (update :stack (fn [stack] (dissoc-idx stack op-data)))
             (update
              :pointer
              (fn [pointer]
                (if (and (> pointer 0) (<= op-data pointer)) (dec pointer) pointer))))))))

(defn save-files [db op-data sid op-id op-time]
  (let [user-id (get-in db [:sessions sid :user-id])
        user-name (get-in db [:users user-id :name])]
    (-> db
        (assoc :saved-files (get-in db [:ir :files]))
        (update
         :sessions
         (fn [sessions]
           (->> sessions
                (map
                 (fn [entry]
                   (let [[k session] entry]
                     [k
                      (update
                       session
                       :notifications
                       (push-info op-id op-time (str user-name " saved files!")))])))
                (into {})))))))

(defn select [db op-data session-id op-id op-time]
  (let [bookmark op-data]
    (-> db
        (update-in [:sessions session-id :writer] (push-bookmark bookmark))
        (assoc-in [:sessions session-id :router] {:name :editor}))))
