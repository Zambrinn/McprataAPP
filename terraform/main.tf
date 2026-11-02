resource "aws_sqs_queue" "payment_dlq" {
  name                      = var.dlq_name
  message_retention_seconds = 1209600

  tags = {
    Role = "DeadLetterQueue"
  }
}

resource "aws_sqs_queue" "payment_queue" {
  name                       = var.queue_name
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.payment_dlq.arn
    maxReceiveCount     = 3
  })

  tags = {
    Role = "PaymentProcessingQueue"
  }
}
resource "aws_iam_policy" "ecs_sqs_payment_policy" {
  name        = "mcprata-ecs-sqs-payment-policy"
  description = "Permite que o container ECS do MCPRATA produza e consuma pagamentos do SQS"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowSQSPaymentOperations"
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl"
        ]
        Resource = [
          aws_sqs_queue.payment_queue.arn,
          aws_sqs_queue.payment_dlq.arn
        ]
      }
    ]
  })
}

