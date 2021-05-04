from django.contrib import admin

from .models import *


class AwsAccountAdmin(admin.ModelAdmin):
    list_display = ("user", "access_key", "secret_key", "token")


admin.site.register(AwsAccount, AwsAccountAdmin)


class NodeInline(admin.TabularInline):
    model = Node
    extra = 0


class WorkflowAdmin(admin.ModelAdmin):
    list_display = ("user", "name")
    inlines = [NodeInline]


admin.site.register(Workflow, WorkflowAdmin)
