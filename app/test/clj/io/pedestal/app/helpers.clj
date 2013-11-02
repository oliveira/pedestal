(ns io.pedestal.app.helpers
  (:require [clojure.core.async :refer [alts!! timeout]]))

(defn take-n
  "Helper function which returns n items taken from the given
  channel."
  [n rc]
  (loop [n n
         t (timeout 1000)
         results []]
    (let [[v c] (alts!! [rc t])]
      (if (or (= c t) (= n 1))
        (conj results v)
        (recur (dec n)  t (conj results v))))))

(defn- map-diff [o n prefix]
  (reduce (fn [a [k new-value]]
            (let [old-value (k o)]
              (cond (= old-value new-value)
                    a

                    (and (map? old-value) (map? new-value))
                    (into a (map-diff old-value new-value (conj prefix k)))

                    (and (nil? old-value) (map? new-value))
                    (into a (map-diff old-value new-value (conj prefix k)))

                    (nil? old-value)
                    (conj a [(conj prefix k) :added])

                    :else
                    (conj a [(conj prefix k) :updated]))))
          []
          n))

(defn- reverse-diffs [ds]
  (keep (fn [[p e]] (when (= e :added) [p :removed])) ds))

(defn ideal-change-report
  "Given an old and new data model, this function produces the change
  report that we would like to see. This is a slow and simple
  implementation that we can use to verify that the faster version is
  working correctly."
  [o n]
  (set (map (fn [[p e]] [p e o n]) (into (map-diff o n [])
                                        (reverse-diffs (map-diff n o []))))))
