(ns competite.db.db
  (:require
    [duratom.core :refer [duratom]]))

(def fake-row
  [:tr [:td "10:00AM"] [:td "Patrick Bowen"] [:td "Somebody Else"] [:td "+1"]])
(def fake-row2
  [:tr [:td "10:00AM"] [:td "Somebody Else"] [:td "Patrick Bowen"] [:td "+1"]])

(def schema
{
  :comps {
    0 {
    }
    1 {
      :title  "Karate competition 1"
      :tags   ["Algeria" "national"]
      :admins #{0}
      :referees #{0}
      :desc   "Let's see **who** wins!"
      :views  {
        "+/karate/win/+/+" {:header "Matches" :headers [:tr [:th "Match"] [:th "Winner"]     [:th "Defeated"]]}
        "+/karate/hit/+/+" {:header "Hits"    :headers [:tr [:th "Match"] [:th "Beligerant"] [:th "Defendant"] [:th "Score"]]}
      }
      :events {
        (str (inst-ms (java.util.Date.)) "/karate/hit/match0/Rayane Sekkour") {:text "Rayane made a hit" :row [:tr [:td "11:00AM"] [:td "Rayane"] [:td "Somebody Else"] [:td 1]]}
      }}}
  :next-user-id 1
  :users
  {0 {:id       0
      :email    "email@gmail.com"
      :name     "Patrick"
      :pass     "37a8eec1ce19687d132fe29051dca629d164e2c4958ba141d5f4133a33f0688f"
      :salt     ""}
   1 {:id       1
      :email    "phun@pm.me"
      :name     "Patrick 2"
      :pass     "37a8eec1ce19687d132fe29051dca629d164e2c4958ba141d5f4133a33f0688f"
      :salt     ""}}})

(def db
  (duratom
    :local-file
    :file-path "competite-DB.edn"
    :init schema))