from django.db import models
from django.contrib.auth.models import User
from fernet_fields import EncryptedCharField, EncryptedIntegerField


class AwsAccount(models.Model):
    user = models.ForeignKey(
        User, related_name="aws", on_delete=models.CASCADE, default=None
    )
    access_key = EncryptedCharField(default=None, max_length=255, blank=True)
    secret_key = EncryptedCharField(default=None, max_length=255, blank=True)
    token = EncryptedCharField(default=None, max_length=255, blank=True)


class Workflow(models.Model):
    user = models.ForeignKey(
        User, related_name="wf", on_delete=models.CASCADE, default=None
    )
    name = models.CharField(default=None, max_length=255, blank=True, primary_key=True)
    status = models.CharField(default=None, max_length=255, blank=True)


class Node(models.Model):
    wf = models.ForeignKey(
        Workflow, related_name="node", on_delete=models.CASCADE, default=None
    )
    name = models.CharField(default=None, max_length=255, blank=True, primary_key=True)
