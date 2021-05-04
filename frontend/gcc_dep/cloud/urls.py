from django.urls import path

from . import views

urlpatterns = [
    path("", views.auth, name="sign in"),
    path("signup", views.signup, name="sign up"),
    path("signup_error", views.signupErr, name="sign up error"),
    path("userhome", views.userHome, name="user home"),
    path("userhome/credentials", views.userCreds, name="user credentials"),
    path("logout", views.pagelogout, name="logout"),
    path("about", views.about, name="about us"),
    path("userhome/createwf", views.wfCreate, name="create wf"),
    path("userhome/executewf", views.wfExecute, name="execute wf"),
    path("userhome/execute/<str:wf_name>", views.execute, name="execute"),
    path("userhome/delete/<str:wf_name>", views.delete, name="delete"),
    path("userhome/download/<str:wf_name>", views.download, name="download"),
]
