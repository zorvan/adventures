;; او می رود دامن کشان من زهر تنهایی چشان * دیگر مگیر از من نشان کز دل نشانم می رود
;; سعدی

(ns adventures.clj.routing
  (:require
   [reitit.core :as reitit]
   [reitit.spec :as rs]
   [reitit.http :as rh] ; An module for http-routing using interceptors instead of middleware
   [reitit.ring :as rr]
   [reitit.coercion :as rc]
   [reitit.pedestal :as rp]

   [reitit.dev.pretty :as pretty]
   [reitit.coercion.schema :as rcs]
   [reitit.middleware :as middleware]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.muuntaja :as rrm]

   [ring.adapter.jetty :as jetty]
   [ring.middleware.params :as params]

   [muuntaja.core :as mc]
   [schema.core :as sc]
   [clojure.spec.alpha :as spec]))

;; ---------- Route Parameters
(comment
;  key        description
  :path       Base-path for routes
  :routes     Initial resolved routes (default [])
  :data       Initial route data (default {})
  :spec       clojure.spec definition for a route data, see reitit.spec on how to use this
  :syntax     Path-parameter syntax as keyword or set of keywords (default #{:bracket :colon})
  :expand     Function of arg opts => data to expand route arg to route data (default reitit.core/expand)
  :coerce     Function of route opts => route to coerce resolved route, can throw or return nil
  :compile    Function of route opts => result to compile a route handler
  :validate   Function of routes opts => () to validate route (data) via side-effects
  :conflicts  Function of {route #{route}} => () to handle conflicting routes
  :exception  Function of Exception => Exception to handle creation time exceptions (default reitit.exception/exception)
  :router     Function of routes opts => router to override the actual router implementation
  )

;; ---------- Different Kinds of Routers

(Comment
;     router                      description
 :linear-router              Matches the routes one-by-one starting from the top until a match is found. Slow, but works with all route trees.
 :trie-router                Router that creates a optimized search trie out of an route table. Much faster than :linear-router for wildcard routes. Valid only if there are no Route conflicts.
 :lookup-router              Fast router, uses hash-lookup to resolve the route. Valid if no paths have path or catch-all parameters and there are no Route conflicts.
 :single-static-path-router  Super fast router: string-matches a route. Valid only if there is one static route.
 :mixed-router               Contains two routers: :trie-router for wildcard routes and a :lookup-router or :single-static-path-router for static routes. Valid only if there are no Route conflicts.
 :quarantine-router          Contains two routers: :mixed-router for non-conflicting routes and a :linear-router for conflicting routes.)

(def router
  (reitit/router
   [["/ping" ::ping]
    ["/api/:users" ::users]]
   {:router reitit/linear-router}))

(reitit/router-name router) ; :linear-router


;; ---------- Route Template 1
(def route1 ["/api"
             ["/ping" ::ping]
             ["/user/:id" ::user]])

;; ---------- Route Template 2
(def route2     ["/rpc"
                 ["/pong" ::pong]
                 ["/admin/:name/:scope" ::admin]])

;; ---------- Create a Router and Merging Route1 & Route 2
;; چون مسیرها فقط داده هستند پس به راحتی ترکیب شده و مسیرهای دیگری می سازند
(def router
  (reitit/router
   [route1
    route2]))

;; ---------- Print Flattened Router Tree
;; نمایشی تخت از درخت مسیرها
(reitit/routes router)
;; خروجی :
(comment [["/api/ping" {:name :adventures.routing/ping}]
          ["/api/user/:id" {:name :adventures.routing/user}]
          ["/rpc/pong" {:name :adventures.routing/pong}]
          ["/rpc/admin/:name/:scope" {:name :adventures.routing/admin}]])


;; ---------- Match by 'Path' Routing
(reitit/match-by-path router "/rpc/admin/john/level3")
;; خروجی :
(comment {:template "/rpc/admin/:name/:scope",
          :data {:name :adventures.routing/admin},
          :result nil,
          :path-params {:name "john", :scope "level3"}, ;; Always coerced to String
          :path "/rpc/admin/john/level3"})


;; ---------- Name-based (reverse) Routing
(reitit/route-names router)
;; خروجی :
(comment [:adventures.routing/ping
          :adventures.routing/user
          :adventures.routing/pong
          :adventures.routing/admin])

(reitit/match-by-name router :admin)
;; خروجی : nil (توجه کنید یه :: vs :)

(reitit/match-by-name router ::admin)
;; خروجی : (تطابق کامل نیست چون داده ها ارائه نشده اند partial match, because it needs :required params)
(comment {:template "/rpc/admin/:name/:scope",
          :data {:name :adventures.routing/admin},
          :result nil,
          :path-params nil,
          :required #{:name :scope}})

(reitit/partial-match? (reitit/match-by-name router ::user))
;; خروجی : : true

(reitit/partial-match? (reitit/match-by-name router ::user {:id 2}))
;; خروجی : false

(-> router
    (reitit/match-by-name ::user {:id 2})
    (reitit/match->path {:hey "Amin"}))
;; خروجی : "/api/user/2?hey=Amin"

;; ---------- مهمترین بخش : داده‌های مسیر

(def router
  (reitit/router
   ["/api" {:interceptors [::api]}
    ["/ping" (fn [s] (str "Hi " s " Ping"))]
    ["/admin" {:roles #{:admin}}
     ["/users" ::users]
     ["/db" {:interceptors [::db]
             :roles ^:replace #{:db-admin}}]]]
   {:data {:middleware [::session]}}))

;; بدون درنظر گرفتن خط آخر  که پارامتر دستور است و بالاترین اولویت را خواهد داشت
;; به صورت پیش فزض اسامی به نام و توابع به هندل تفسیر می شوند :name user & :handler #function.
;; مگر اینکه صراحتا ذکر کنیم :interceptor & :roles
(comment [["/api/ping"
           {:interceptors [:adventures.routing/api],
            :handler #function[adventures.routing/fn--32043]}]
          ["/api/admin/users"
           {:interceptors [:adventures.routing/api],
            :roles #{:admin},
            :name :adventures.routing/users}]
          ["/api/admin/db"
           {:interceptors [:adventures.routing/api :adventures.routing/db],
            :roles #{:db-admin}}]])

;; حالا اگر خط آخر یعنی اضافه کردن را در نظر بگیریم
;; TOP LEVEL ROUTE DATA
(comment
  [["/api/ping"
    {:middleware [:adventures.routing/session],
     :interceptors [:adventures.routing/api],
     :handler #function[adventures.routing/fn--32053]}]
   ["/api/admin/users"
    {:middleware [:adventures.routing/session],
     :interceptors [:adventures.routing/api],
     :roles #{:admin},
     :name :adventures.routing/users}]
   ["/api/admin/db"
    {:middleware [:adventures.routing/session],
     :interceptors [:adventures.routing/api :adventures.routing/db],
     :roles #{:db-admin}}]])

;; ---------- اعتبارسنجی
(reitit/router
 ["/api" {:handler "identity"}]
 {:validate rs/validate})
;; اجرای دستور بالا خطای عدم وجود تابع در تعریف هندلر را می دهد


;; ---------- تداخل و خطاهای قابل فهم
(reitit/router
 [["/ping"]
  ["/:user-id/orders"]
  ["/bulk/:bulk-id"]
  ["/public/*path"]
  ["/:version/status"]]
 {:exception pretty/exception})

;; خروجی :

(comment
  -- Router creation failed --------------------------------

  Router contains conflicting route paths:

  -> /:user-id/orders
  -> /public/*path
  -> /bulk/:bulk-id

  -> /bulk/:bulk-id
  -> /:version/status

  -> /public/*path
  -> /:version/status

  Either fix the conflicting paths or disable the conflict resolution
  by setting route data for conflicting route:

  {:conflicting true}

  or by setting a router option:

  {:conflicts nil}

  https://cljdoc.org/d/metosin/reitit/CURRENT/doc/basics/route-conflicts
  )

;; ---------- Coercion تعیین تایپ متغیرها

(def router
  (reitit/router
   ["/:company/users/:user-id" {:name ::user-view
                                :coercion rcs/coercion
                                :parameters {:path {:company sc/Str
                                                    :user-id sc/Int}}}]
   {:compile rc/compile-request-coercers})) ;; کامپایل کردن

(defn match-by-path-and-coerce! [path]
  (if-let [match (reitit/match-by-path router path)]
    (assoc match :parameters (rc/coerce! match))))

(match-by-path-and-coerce! "/metosin/users/123")
                                        ; خروجی (تشخیص اینکه ۱۲۳ عدد است و نه رشته)
(comment {:template "/:company/users/:user-id",
          :data
          {:name :adventures.routing/user-view,
           :coercion #Coercion{:name :schema},
           :parameters {:path {:company java.lang.String, :user-id Int}}},
          :result {:path #function[reitit.coercion/request-coercer/fn--19166]},
          :path-params {:company "metosin", :user-id "123"},
          :path "/metosin/users/123",
          :parameters {:path {:company "metosin", :user-id 123}}})


(match-by-path-and-coerce! "/metosin/users/ikitommi")
; خروجی : ...reitit.coercion/request-coercion-failed! ...

;; ---------- RING

; مثال مهم : Data Driven Middleware (مانند Duct)


; wrap (Function) ,wrap2(Record) and wrap3 (Map) are all equal
(defn wrap [handler id]
  (fn [request]
    (handler (update request ::acc (fnil conj []) id))))

(def wrap2
  (middleware/map->Middleware
   {:name ::wrap2
    :description "Middleware that does things."
    :wrap wrap}))

(def wrap3
  {:name ::wrap3
   :description "Middleware that does things."
   :wrap wrap})

; Usage
(defn handler [{::keys [acc]}]
  {:status 200, :body (conj acc :handler)})

(def app
  (rr/ring-handler
   (rr/router
    ["/api" {:middleware [[wrap 1] [wrap2 2]]}
     ["/ping" {:get {:middleware [[wrap3 3]]
                     :handler handler}}]])))

(app {:request-method :get, :uri "/api/ping"})
; خروجی :
(comment
  {:status 200, :body [1 2 3 :handler]})


; مثال مهم : Compiled Middleware
; روشی که هم خواناتر و هم سریع تر  است
(require '[buddy.auth.accessrules :as accessrules])

(spec/def ::authorize
  (spec/or :handler :accessrules/handler :rule :accessrules/rule))

; استفاده از spec در Compiled Middleware
; because of :spec there must be :authorize keyword in receiving request
(def authorization-middleware
  {:name ::authorization
   :spec (spec/keys :req-un [::authorize])
   :compile
   (fn [route-data _opts]
     (when-let [rule (:authorize route-data)]
       (fn [handler]
         (accessrules/wrap-access-rules handler {:rules [rule]}))))})




;; ---------- Pedestal
(require '[io.pedestal.http :as server])

(defn interceptor [number]
  {:enter (fn [ctx] (update-in ctx [:request :number] (fnil + 0) number))})

(def routes
  ["/api"
   {:interceptors [(interceptor 1)]}

   ["/number"
    {:interceptors [(interceptor 10)]
     :get {:interceptors [(interceptor 100)]
           :handler (fn [req]
                      {:status 200
                       :body (select-keys req [:number])})}}]])

(-> {::server/type :jetty
     ::server/port 3000
     ::server/join? false
     ;; no pedestal routes
     ::server/routes []}
    (server/default-interceptors)
    ;; swap the reitit router
    (rp/replace-last-interceptor
     (rp/routing-interceptor
      (rh/router routes)))
    (server/dev-interceptors)
    (server/create-server)
    (server/start))

; Use this command on bash to communicate with server :
; $ http -v GET 127.0.0.1:3000/api/number
; output will be {:number 111}


;; ---------- Siepari Interseptors
(require '[reitit.interceptor.sieppari :as sieppari])
(require '[sieppari.async.core-async]) ;; needed for core.async
(require '[sieppari.async.manifold])  ;; needed for manifold

; Synchronous Ring
(defn i [x]
  {:enter (fn [ctx] (println "enter " x) ctx)
   :leave (fn [ctx] (println "leave " x) ctx)})

(defn handler [_]
  (future {:status 200, :body "pong"}))

(def app
  (http/ring-handler
   (http/router
    ["/api"
     {:interceptors [(i :api)]}

     ["/ping"
      {:interceptors [(i :ping)]
       :get {:interceptors [(i :get)]
             :handler handler}}]])
   {:executor sieppari/executor}))

(app {:request-method :get, :uri "/api/ping"})

; خروجی :
(comment
  enter  :api
  enter  :ping
  enter  :get
  leave  :get
  leave  :ping
  leave  :api
  {:status 200, :body "pong"})

; Async Ring
(let [respond (promise)]
  (app {:request-method :get, :uri "/api/ping"} respond nil)
  (deref respond 1000 ::timeout))
; خروجی :‌
(commnet
 enter  :api
 enter  :ping
 enter  :get
 leave  :get
 leave  :ping
 leave  :api
 {:status 200, :body "pong"})

;; ---------- Advanced : Composing Routers

; Adding Routes
(defn add-routes [router routes]
  (reitit/router
   (into (reitit/routes router) routes)
   (reitit/options router)))

(def router
(reitit/router
 [["/foo" ::foo]
  ["/bar/:id" ::bar]]))

(def router2
  (add-routes
   router
   [["/baz/:id/:subid" ::baz]]))

; Merging Routes
(defn merge-routers [& routers]
  (reitit/router
   (apply merge (map reitit/routes routers))
   (apply merge (map reitit/options routers))))

(def router
  (merge-routers
   (reitit/router ["/route1" ::route1])
   (reitit/router ["/route2" ::route2])
   (reitit/router ["/route3" ::route3])))

; Nesting routers (11x-15x کندتر از حالت استاتیک)
(def router
  (reitit/router
   [["/ping" :ping]
    ["/olipa/*" {:name :olipa
                 :router (reitit/router
                          [["/olut" :olut]
                           ["/makkara" :makkara]
                           ["/kerran/*" {:name :kerran
                                         :router (reitit/router
                                                  [["/avaruus" :avaruus]
                                                   ["/ihminen" :ihminen]])}]])}]])) ; match-by-path  به درستی کار نمی کند و باید تابعی برای جستجوی تو در تو نوشت

(require '[clojure.string :as str])

(defn- << [x]
  (if (instance? clojure.lang.IDeref x)
    (deref x) x))

(defn recursive-match-by-path [router path]
  (if-let [match (r/match-by-path (<< router) path)]
    (if-let [subrouter (-> match :data :router <<)]
      (let [subpath (subs path (str/last-index-of (:template match) "/"))]
        (if-let [submatch (recursive-match-by-path subrouter subpath)]
          (cons match submatch)))
      (list match))))

(defn name-path [router path]
  (some->> (recursive-match-by-path router path)
           (mapv (comp :name :data))))

; Dynamic Routers (30x کندتر از حالت استاتیک)

(def beer-router
  (atom
   (r/router
    [["/lager" :lager]])))

(def dynamic-router
  (reify clojure.lang.IDeref
    (deref [_]
      (r/router
       ["/duo" (keyword (str "duo" (rand-int 100)))]))))

(def router
  (r/router
   [["/gin/napue" :napue] ; static ( 23ns : match-by-path) (40ns : recursive-match-by-path)
    ["/ciders/*" :ciders] ; catch-all (* علامت) (440ns زمان)
    ["/beers/*" {:name :beers ; Catch-all + static (600ns زمان)
                 :router beer-router}]
    ["/dynamic/*" {:name :dynamic  ;  Catch-all + Dynamic (1200ns زمان)
                   :router dynamic-router}]]))


(name-path router "/vodka/russian") ; nil
(name-path router "/gin/napue") ; [:napue]
(name-path router "/beers/lager") ; [:beers :lager]

(name-path router "/beers/saison") ; nil
(swap! beer-router add-routes [["/saison" :saison]])
(name-path router "/beers/saison") ; [:beers :saison]

 ; هربار تولید می شود چون به صورت عملیات رشته ای نوشته شده
(name-path router "/dynamic/duo"); [:dynamic :duo71]
(name-path router "/dynamic/duo"); [:dynamic :duo55]


; NOTE : Nesting routers is not trivial and because of that, should be avoided.
; For dynamic (request-time) route generation, it's the only choice.
; For other cases, nested routes are most likely a better option.

;; ---------- Router Validation
(require '[expound.alpha :as expound])

(def routes-from-db
  ["tenant1" ::tenant1])

(spec/valid? ::rs/raw-routes routes-from-db)  ; false
(spec/explain ::rs/raw-routes routes-from-db)

(require '[clojure.spec.test.alpha :as stest])
(stest/instrument `reitit/router)
(set! spec/*explain-out* print)

(reitit/router
 ["/api"
  ["/public"
   ["/ping"]
   ["pong"]]])

;; ----------

;; ----------

;; ----------

;; ----------

;; ----------
