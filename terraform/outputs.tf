output "payment_queue_url" {
  description = "URL da fila principal SQS para uso no Spring Boot"
  value       = aws_sqs_queue.payment_queue.url
}

output "payment_queue_arn" {
  description = "ARN da fila principal SQS"
  value       = aws_sqs_queue.payment_queue.arn
}

output "payment_dlq_url" {
  description = "URL da Dead Letter Queue"
  value       = aws_sqs_queue.payment_dlq.url
}

output "payment_dlq_arn" {
  description = "ARN da Dead Letter Queue"
  value       = aws_sqs_queue.payment_dlq.arn
}

output "ecs_sqs_policy_arn" {
  description = "ARN da política IAM criada para anexar na Task Role do ECS"
  value       = aws_iam_policy.ecs_sqs_payment_policy.arn
}
