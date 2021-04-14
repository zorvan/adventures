(ns adventures.clj.scrapper
  (:require
   [etaoin.api  :as wd]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as st]
   ))

(def NumJobs 30)
(def URL "https://www.irantalent.com/jobs/it-software-web-development-jobs?language=english&page=")
(def Titles ["job descript" "requirement" "job categor" "employment type" "seniorit"])
(def JobXPath "/html/body/app-root/div/div/div[2]/div/app-new-advanced-search/div/div/div[2]/div[1]/div/div[3]/div")
(def LastPageXPath "/html/body/app-root/div/div/div[2]/div/app-new-advanced-search/div/div/div[2]/div[1]/div/div[3]/div[31]/it-pagination/div/div[1]/div/button[5]")

(defn reindexof [v x]
  (first
    (keep-indexed #(if (some? %2) %1)
                  (mapv #(re-matches (re-pattern (str x ".*")) %) v))))

(defn parser [text]
  (let [v        (st/split-lines (st/lower-case text))
        jobtitle (v 0)
        company  (v 1)
        city     (v 2)
        iv       (mapv #(reindexof v %) Titles)]
    [jobtitle company city
     (st/join (mapv v (range (inc (iv 0)) (iv 1)))) ; "job description"
     (st/join (mapv v (range (inc (iv 1)) (iv 2)))) ; "requirements"
     (st/join (mapv v (range (inc (iv 2)) (iv 3)))) ; "job category"
     (st/join (mapv v (range (inc (iv 3)) (iv 4)))) ; "employment type"
     (st/join (mapv v (range (inc (iv 4)) (count v))))]))

(defn gopage [driver pagenum]
  (do
    (wd/go driver (str URL pagenum))
    (wd/wait driver 3)
    (wd/wait-visible driver [{:id :selected-job-preview}])))

(defn readjob [driver jobnum]
  (do
    (wd/click driver (str JobXPath"["jobnum"]"))
    (wd/wait driver 1)
    (wd/wait-visible driver [{:id :selected-job-preview}])
    (parser (wd/get-element-text driver ".//*[@id='selected-job-preview']"))))

(defn job->csv [filename v]
  (with-open [writer (io/writer filename)]
    (csv/write-csv writer v)))

;; here, a chrome window should appear
;; open chrome and URL at page=1
(def driver (wd/chrome))
(gopage driver 1)
(def NumPages (Integer/parseInt (wd/get-element-text driver LastPageXPath)))

(loop [pagenum 1 jobnum 1 jobs [["Job Title" "Company" "City" "Description" "Requirements" "Category" "Type" "Seniority"]]]
  (if (= pagenum NumPages)
    (job->csv (str "jobs.csv") jobs)
    (if (<= jobnum NumJobs)
      (recur pagenum
             (inc jobnum)
             (conj jobs (readjob driver jobnum)))
      (do
        (gopage driver (inc pagenum))
        (recur (inc pagenum) 1 jobs))
      )))

;; stops Chrome and HTTP server
(wd/quit driver)
