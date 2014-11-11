(ns lambdacd.pipeline-state
  "responsible to manage the current state of the pipeline
  i.e. what's currently running, what are the results of each step, ..."
  (:import (java.io File))
  (:require [clojure.core.async :as async]
            [lambdacd.util :as util]
            [lambdacd.json-model :as json-model]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def clean-pipeline-state {})

(defn- read-state [filename]
  (let [build-number (read-string (second (re-find #"build-(\d+)" filename)))
        state (json-model/json-format->pipeline-state (json/read-str (slurp filename) :key-fn keyword))]
    { build-number state }))

(defn initial-pipeline-state [{ home-dir :home-dir }]
  (let [dir (io/file home-dir)
        home-contents (file-seq dir)
        directories-in-home (filter #(.isDirectory %) home-contents)
        build-dirs (filter #(.startsWith (.getName %) "build-") directories-in-home)
        build-state-files (map #(str % "/pipeline-state.json") build-dirs)
        states (map read-state build-state-files)]
    (into {} states)))

(defn- update-current-run [step-id step-result current-state]
  (assoc current-state step-id step-result))

(defn- update-pipeline-state [build-number step-id step-result current-state]
  (assoc current-state build-number (update-current-run step-id step-result (get current-state build-number))))

(defn current-build-number [{pipeline-state :_pipeline-state }]
  (if-let [current-build-number (last (sort (keys @pipeline-state)))]
    current-build-number
    0))

(defn- write-state-to-disk [home-dir build-number new-state]
  (if home-dir
  (let [dir (str home-dir "/" "build-" build-number "/")
        path (str dir "pipeline-state.json")
        state-as-json (json-model/pipeline-state->json-format (get new-state build-number))]
    (.mkdirs (io/file dir))
    (util/write-as-json path state-as-json))))

(defn update [{step-id :step-id state :_pipeline-state build-number :build-number { home-dir :home-dir } :config } step-result]
  (if (not (nil? state)) ; convenience for tests: if no state exists we just do nothing
    (let [new-state (swap! state (partial update-pipeline-state build-number step-id step-result))]
      (write-state-to-disk home-dir build-number new-state))))

(defn running [ctx]
  (update ctx {:status :running}))
