from django.shortcuts import render
from django.http import HttpResponse, HttpResponseRedirect, FileResponse

from cloud.models import *
from cloud.forms import *

from django.template import loader
from django.contrib.auth.models import User
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.core.files.storage import default_storage
from django.conf import settings
from django.core.files.base import ContentFile

import os
import sys
import shutil
import traceback
import subprocess
import paramiko
import threading
import socket
import time


def auth(request):
    if request.method == "POST":
        form = SignInForm(request.POST)
        if form.is_valid():
            entered_usr = request.POST.get("user")
            entered_pwd = request.POST.get("pwd")
            user = authenticate(username=entered_usr, password=entered_pwd)
            if user is not None:
                login(request, user)
                return HttpResponseRedirect("/userhome")
            else:
                return HttpResponseRedirect("/")
    else:
        form = SignInForm()

    template = loader.get_template("cloud/auth.html")
    context = {"form": form}
    return HttpResponse(template.render(context, request))


def signup(request):
    if request.method == "POST":
        form = SignUpForm(request.POST)
        if form.is_valid():
            usr = request.POST.get("user")
            pwd = request.POST.get("pwd2")
            email = request.POST.get("email")
            try:
                user = User.objects.create_user(username=usr, password=pwd, email=email)
                login(request, user)

                ac = AwsAccount()
                ac.user = user
                ac.access_key = ""
                ac.secret_key = ""
                ac.token = ""

                ac.save()

                os.mkdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}")
                os.mkdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows")
                os.mkdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions")
                subprocess.call(["sudo", "chmod", "-R", "777", f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}"])

                return HttpResponseRedirect("/userhome/credentials")

            except Exception as e:
                return HttpResponseRedirect("/signup_error")
    else:
        form = SignUpForm()

    template = loader.get_template("cloud/signup.html")
    context = {
        "form": form,
    }
    return HttpResponse(template.render(context, request))


def signupErr(request):
    if request.method == "POST":
        form = SignUpForm(request.POST)
        if form.is_valid():
            usr = request.POST.get("user")
            pwd = request.POST.get("pwd2")
            email = request.POST.get("email")
            try:
                user = User.objects.create_user(username=usr, password=pwd, email=email)
                login(request, user)

                ac = AwsAccount()
                ac.user = user
                ac.access_key = ""
                ac.secret_key = ""
                ac.token = ""

                ac.save()

                os.mkdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}")
                os.mkdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows")
                os.mkdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions")
                subprocess.call(["sudo", "chmod", "-R", "777", f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}"])

                return HttpResponseRedirect("/userhome/credentials")

            except Exception as e:
                return HttpResponseRedirect("/signup_error")
    else:
        form = SignUpForm()

    template = loader.get_template("cloud/signup_error.html")
    context = {
        "form": form,
    }
    return HttpResponse(template.render(context, request))


def about(request):
    template = loader.get_template("cloud/about.html")
    context = {}
    return HttpResponse(template.render(context, request))


@login_required
def userCreds(request):
    if len(AwsAccount.objects.filter(user=request.user)) > 0:
        ak = AwsAccount.objects.filter(user=request.user)[0].access_key
        sk = AwsAccount.objects.filter(user=request.user)[0].secret_key
        tok = AwsAccount.objects.filter(user=request.user)[0].token
    else:
        ak = ""
        sk = ""
        tok = ""

    if request.method == "POST":
        form = CredentialForm(request.POST)
        if form.is_valid():
            AwsAccount.objects.filter(user=request.user).delete()

            ac = AwsAccount()
            ac.user = request.user
            ac.access_key = request.POST.get("access_key")
            ac.secret_key = request.POST.get("secret_key")
            ac.token = request.POST.get("token")

            ac.save()

        return HttpResponseRedirect("/userhome")

    else:
        form = CredentialForm()
        form.fields["access_key"].initial = ak
        form.fields["secret_key"].initial = sk
        form.fields["token"].initial = tok

    template = loader.get_template("cloud/userCreds.html")
    context = {
        "user": request.user,
        "form": form,
    }
    return HttpResponse(template.render(context, request))


@login_required
def userHome(request):
    template = loader.get_template("cloud/userHome.html")
    context = {
        "user": request.user,
    }
    return HttpResponse(template.render(context, request))


@login_required
def wfCreate(request):
    workflows = Workflow.objects.filter(user=request.user)

    if request.method == "POST":
        form = WorkflowForm(request.POST, request.FILES)

        if form.is_valid():
            spec_file = request.FILES["spec"]
            node_files = request.FILES.getlist("nodes")

            wf_model = Workflow(user=request.user, name=spec_file.name.strip(".xml"), status="dormant")
            wf_model.save()

            if not os.path.isdir(
                f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}"
            ):
                os.mkdir(
                    f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}"
                )
                subprocess.call(["sudo", "chmod", "-R", "777", f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}"])
            else:
                shutil.rmtree(
                    f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}"
                )
                os.mkdir(
                    f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}"
                )
                subprocess.call(["sudo", "chmod", "-R", "777", f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}"])

            with open(
                f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}/{spec_file.name}",
                "wb+",
            ) as spec:
                spec_file_content = ContentFile(spec_file.read())
                try:
                    for chunk in spec_file_content.chunks():
                        spec.write(chunk)
                    spec.close()
                except:
                    return HttpResponseRedirect("/userhome/createwf")

            try:
                os.mkdir(
                    f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}/nodes"
                )
                subprocess.call(["sudo", "chmod", "-R", "777", f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}/nodes"])
            except:
                return HttpResponseRedirect("/userhome/createwf")

            for node_file in node_files:
                node_model = Node(wf=wf_model, name=node_file.name.strip(".zip"))
                node_model.save()

                try:
                    os.mkdir(
                        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}/nodes/{node_file.name.strip('.zip')}"
                    )
                    subprocess.call(["sudo", "chmod", "-R", "777", f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}/nodes/{node_file.name.strip('.zip')}"])
                except:
                    return HttpResponseRedirect("/userhome/createwf")

                with open(
                    f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{spec_file.name.strip('.xml')}/nodes/{node_file.name.strip('.zip')}/{node_file.name}",
                    "wb+",
                ) as node:
                    node_file_content = ContentFile(node_file.read())
                    try:
                        for chunk in node_file_content:
                            node.write(chunk)
                        node.close()
                    except:
                        return HttpResponseRedirect("/userhome/createwf")

            sync(
                f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}",
                f"ubuntu@{os.environ.get('EXEC_HOSTNAME')}:/home/ubuntu/USERS"
            )

        return HttpResponseRedirect("/userhome/createwf")

    else:
        form = WorkflowForm()

    template = loader.get_template("cloud/wfCreate.html")
    context = {
        "form": form,
    }
    return HttpResponse(template.render(context, request))


@login_required
def wfExecute(request):
    workflows = Workflow.objects.filter(user_id=request.user.id)
    for wf in workflows:
        nodes = wf.node.all()
        wf_name = wf.name

        sync(
            f"ubuntu@{os.environ.get('EXEC_HOSTNAME')}:/home/ubuntu/USERS/{request.user.username}/executions/{wf_name}",
            f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions"
        )

        if os.path.isdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf.name}/logs"):
            log_len = len(os.listdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf.name}/logs"))
        else:
            log_len = 0

        if os.path.isdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf.name}/logs"):
            res_len = len(os.listdir(f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf.name}/results"))
        else:
            res_len = 0

        if log_len == len(nodes) and res_len != 0:
            workflows.filter(name=wf.name).update(status="success")
        elif log_len == len(nodes) and res_len == 0:
            workflows.filter(name=wf.name).update(status="failed")


    template = loader.get_template("cloud/wfExecute.html")
    context = {
        "user": request.user,
        "workflows": workflows,
    }
    return HttpResponse(template.render(context, request))


@login_required
def pagelogout(request):
    if request.method == "POST":
        logout(request)
    return HttpResponseRedirect("/")


@login_required
def execute(request, wf_name):
    workflows = Workflow.objects.filter(user_id=request.user.id)
    wf = workflows.filter(name=wf_name).update(status="executing")

    paths = [
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf_name}",
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf_name}/pem",
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf_name}/logs",
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf_name}/results"
    ]

    for p in paths:
        if not os.path.isdir(p):
            os.mkdir(p)
            subprocess.call(["sudo", "chmod", "-R", "777", p])
        else:
            shutil.rmtree(p)
            os.mkdir(p)
            subprocess.call(["sudo", "chmod", "-R", "777", p])

    sync(
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}",
        f"ubuntu@{os.environ.get('EXEC_HOSTNAME')}:/home/ubuntu/USERS"
    )

    print(f"./exec.sh {request.user.username} {wf_name} {AwsAccount.objects.filter(user=request.user)[0].access_key} {AwsAccount.objects.filter(user=request.user)[0].secret_key} {AwsAccount.objects.filter(user=request.user)[0].token}")

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.MissingHostKeyPolicy())
    client.connect(os.environ.get("EXEC_HOSTNAME"), username="ubuntu", password=os.environ.get("EXEC_PASS"))
    transport = client.get_transport()
    channel = transport.open_session()
    channel.exec_command(f"./exec.sh {request.user.username} {wf_name} {AwsAccount.objects.filter(user=request.user)[0].access_key} {AwsAccount.objects.filter(user=request.user)[0].secret_key} {AwsAccount.objects.filter(user=request.user)[0].token}")

    return HttpResponseRedirect("/userhome/executewf")


@login_required
def delete(request, wf_name):
    wf = Workflow.objects.filter(user=request.user, name=wf_name)
    wf.delete()

    paths = [
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/executions/{wf_name}",
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}/workflows/{wf_name}"
    ]

    for p in paths:
        if os.path.isdir(p):
            shutil.rmtree(p)

    sync(
        f"/home/ubuntu/gcc/gcc_dep/exec/{request.user.username}",
        f"ubuntu@{os.environ.get('EXEC_HOSTNAME')}:/home/ubuntu/USERS"
    )

    return HttpResponseRedirect("/userhome/executewf")


@login_required
def download(request, wf_name):
    subprocess.call([f"/home/ubuntu/gcc/gcc_dep/zip.sh", request.user.username, wf_name, f"{request.user.username}_{wf_name}_results.zip"])
    if os.path.isfile(f"/home/ubuntu/gcc/gcc_dep/temp/{request.user.username}_{wf_name}_results.zip"):
        try:
            return FileResponse(open(f"/home/ubuntu/gcc/gcc_dep/temp/{request.user.username}_{wf_name}_results.zip", "rb"), content_type='application/force-download')
        finally:
            os.remove(f"/home/ubuntu/gcc/gcc_dep/temp/{request.user.username}_{wf_name}_results.zip")
    else:
            return HttpResponseRedirect("/userhome/executewf")


def sync(src_path, dest_path):
    subprocess.Popen([f"/home/ubuntu/gcc/gcc_dep/sync.sh", os.environ.get("EXEC_PASS"), src_path, dest_path], stdin=subprocess.PIPE)
