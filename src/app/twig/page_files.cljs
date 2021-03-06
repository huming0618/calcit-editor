
(ns app.twig.page-files
  (:require [recollect.twig :refer [deftwig]]
            [clojure.set :refer [union]]
            [app.util :refer [file->cirru]]))

(defn compare-entry [new-x old-x]
  (cond
    (and (nil? old-x) (some? new-x)) :add
    (and (some? old-x) (nil? new-x)) :remove
    (and (some? old-x) (some? new-x) (not (identical? old-x new-x))) :changed
    :else :same))

(defn keys-set [x] (set (keys x)))

(defn render-changed-files [files saved-files]
  (->> (union (keys-set files) (keys-set saved-files))
       (filter
        (fn [ns-text] (not (identical? (get files ns-text) (get saved-files ns-text)))))
       (map
        (fn [ns-text]
          (let [file (get files ns-text), saved-file (get saved-files ns-text)]
            [ns-text
             {:ns (compare-entry (:ns file) (:ns saved-file)),
              :proc (compare-entry (:proc file) (:proc saved-file)),
              :defs (let [all-defs (union
                                    (keys-set (:defs file))
                                    (keys-set (:defs saved-file)))
                          defs (:defs file)
                          saved-defs (:defs saved-file)]
                (->> all-defs
                     (filter
                      (fn [def-text] (not= (get defs def-text) (get saved-defs def-text))))
                     (map
                      (fn [def-text]
                        [def-text
                         (compare-entry (get defs def-text) (get saved-defs def-text))]))
                     (into {})))}])))
       (into {})))

(deftwig
 twig-page-files
 (files selected-ns saved-files draft-ns sessions sid)
 {:ns-set (into #{} (keys files)),
  :defs-set (if (some? selected-ns)
    (do (->> (get-in files [selected-ns :defs]) (keys) (into #{})))
    #{}),
  :changed-files (render-changed-files files saved-files),
  :peeking-file (if (some? draft-ns) (file->cirru (get files draft-ns)) nil),
  :highlights (->> sessions
                   (map
                    (fn [[k session]]
                      [k
                       (let [writer (:writer session)]
                         (dissoc (get (:stack writer) (:pointer writer)) :focus))]))
                   (filter (fn [[k session]] (if (= sid k) false (some? session))))
                   (into {}))})
